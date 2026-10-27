package dev.jianwii.notification.service;

import dev.jianwii.entity.notification.EmailMessage;
import dev.jianwii.entity.notification.EmailStatus;
import dev.jianwii.entity.notification.Notification;
import dev.jianwii.notification.dao.EmailMessageDAO;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 把通知轉成待寄信件寫進資料表（outbox），並發出 {@link EmailQueuedEvent}。 必須在呼叫端的交易內執行：信件與通知一起 commit 或
 * rollback，commit 後 {@link EmailDispatcher} 才寄。
 */
@Component
public class EmailOutbox {
    private static final String SUBJECT_PREFIX = "[spring-core-lab] ";

    private final EmailMessageDAO emailMessageDAO;
    private final ApplicationEventPublisher eventPublisher;
    private final String frontendUrl;
    private final boolean enabled;

    public EmailOutbox(
            EmailMessageDAO emailMessageDAO,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail.enabled:true}") boolean enabled) {
        this.emailMessageDAO = emailMessageDAO;
        this.eventPublisher = eventPublisher;
        // 連結是 / 開頭的路徑，去掉網址結尾的 / 避免變成 //
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.enabled = enabled;
    }

    /**
     * @param emails 收件人 id → Email，notifications 的收件人都必須在其中
     */
    public void enqueue(List<Notification> notifications, Map<String, String> emails) {
        if (!enabled || notifications.isEmpty()) {
            return;
        }
        List<EmailMessage> messages = notifications.stream()
                .map(notification -> {
                    EmailMessage message = new EmailMessage();
                    message.setNotificationId(notification.getId());
                    message.setRecipient(emails.get(notification.getRecipientId()));
                    message.setSubject(SUBJECT_PREFIX + notification.getTitle());
                    message.setBody(body(notification));
                    message.setStatus(EmailStatus.PENDING);
                    message.setAttempts(0);
                    return message;
                })
                .toList();
        List<Long> ids = emailMessageDAO.saveAll(messages).stream()
                .map(EmailMessage::getId)
                .toList();
        // @TransactionalEventListener 會等到交易 commit 才收到；rollback 就收不到，不會寄出
        eventPublisher.publishEvent(new EmailQueuedEvent(ids));
    }

    /** 純文字內文：標題、內容、站內連結。 */
    private String body(Notification notification) {
        StringBuilder body = new StringBuilder(notification.getTitle()).append("\n\n");
        if (StringUtils.hasText(notification.getContent())) {
            body.append(notification.getContent()).append("\n\n");
        }
        if (StringUtils.hasText(notification.getLink())) {
            body.append("前往查看：")
                    .append(frontendUrl)
                    .append(notification.getLink())
                    .append("\n\n");
        }
        return body.append("此信由系統自動發送，請勿直接回覆。").toString();
    }
}
