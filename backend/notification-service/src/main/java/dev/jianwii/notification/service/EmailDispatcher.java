package dev.jianwii.notification.service;

import dev.jianwii.entity.notification.EmailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 寄信。兩個入口都走 {@link #dispatch}：
 * <ul>
 * <li>{@link #onQueued}：寫入通知的交易 commit 後，在背景執行緒馬上寄（呼叫端不必等 SMTP）。</li>
 * <li>{@link #retry}：排程重寄失敗或漏寄的信。</li>
 * </ul>
 * 先「領取」再寄（{@link EmailMessageService#claim}），兩個入口同時碰到同一封信也只會寄一次。
 * 多個實例同時跑排程也一樣靠領取的樂觀鎖擋住。
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true", matchIfMissing = true)
public class EmailDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final JavaMailSender mailSender;
    private final EmailMessageService emailMessageService;
    private final String from;

    public EmailDispatcher(JavaMailSender mailSender, EmailMessageService emailMessageService,
            @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.emailMessageService = emailMessageService;
        this.from = from;
    }

    /** 預設 phase 為 AFTER_COMMIT：交易成功才寄；加 @Async 改在 applicationTaskExecutor 的執行緒執行。 */
    @Async
    @TransactionalEventListener
    public void onQueued(EmailQueuedEvent event) {
        dispatchAll(event.ids());
    }

    @Scheduled(fixedDelayString = "${app.mail.retry-interval}",
            initialDelayString = "${app.mail.retry-interval}")
    public void retry() {
        dispatchAll(emailMessageService.findRetryableIds());
    }

    private void dispatchAll(List<Long> ids) {
        for (Long id : ids) {
            try {
                dispatch(id);
            } catch (RuntimeException e) {
                // 例如資料庫暫時連不上；留給下次排程，不影響同一批的其他信
                log.error("寄信處理失敗，id：{}", id, e);
            }
        }
    }

    private void dispatch(Long id) {
        EmailMessage message;
        try {
            message = emailMessageService.claim(id).orElse(null);
        } catch (OptimisticLockingFailureException e) {
            // 別的執行緒先領走了
            return;
        }
        if (message == null) {
            return;
        }
        try {
            mailSender.send(toMimeMessage(message));
            emailMessageService.markSent(id);
        } catch (MailException | MessagingException e) {
            log.warn("寄信失敗（第 {} 次），id：{}，收件人：{}", message.getAttempts(), id,
                    message.getRecipient(), e);
            emailMessageService.markFailed(id, e.getMessage());
        }
    }

    private MimeMessage toMimeMessage(EmailMessage message) throws MessagingException {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(message.getRecipient());
        helper.setSubject(message.getSubject());
        helper.setText(message.getBody());
        return mime;
    }
}
