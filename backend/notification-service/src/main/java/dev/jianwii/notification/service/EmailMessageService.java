package dev.jianwii.notification.service;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.entity.notification.EmailMessage;
import dev.jianwii.entity.notification.EmailStatus;
import dev.jianwii.notification.dao.EmailMessageDAO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 寄信狀態的變更，每個方法各自一個短交易；寄信本身（連 SMTP）在交易外，由 {@link EmailDispatcher} 執行， 不會在等 SMTP 時占著資料庫連線。
 */
@Service
@Transactional
public class EmailMessageService {
    /** PENDING 超過這段時間還沒寄，視為 commit 後的非同步寄送沒執行到。 */
    private static final Duration PENDING_GRACE = Duration.ofMinutes(1);
    /** 失敗後至少隔這段時間再重試。 */
    private static final Duration FAILED_BACKOFF = Duration.ofMinutes(1);
    /** SENDING 超過這段時間，視為寄到一半服務掛掉；SMTP 逾時設定遠小於此值。 */
    private static final Duration SENDING_TIMEOUT = Duration.ofMinutes(10);
    /** 每次排程最多處理幾封。 */
    private static final int BATCH_SIZE = 100;

    private final EmailMessageDAO emailMessageDAO;
    private final int maxAttempts;

    public EmailMessageService(EmailMessageDAO emailMessageDAO,
            @Value("${app.mail.max-attempts}") int maxAttempts) {
        this.emailMessageDAO = emailMessageDAO;
        this.maxAttempts = maxAttempts;
    }

    @Transactional(readOnly = true)
    public List<Long> findRetryableIds() {
        LocalDateTime now = LocalDateTime.now();
        return emailMessageDAO.findRetryableIds(maxAttempts, now.minus(PENDING_GRACE),
                now.minus(FAILED_BACKOFF), now.minus(SENDING_TIMEOUT),
                PageRequest.of(0, BATCH_SIZE));
    }

    /**
     * 領取一封要寄的信：改成 SENDING 並把次數加一。不需要寄（已寄出、別人正在寄、次數用完）時回 empty。 兩個執行緒同時領取同一封時，後存檔的版本不符，丟
     * OptimisticLockingFailureException，由呼叫端放棄。
     */
    public Optional<EmailMessage> claim(Long id) {
        EmailMessage message = emailMessageDAO.findById(id)
                .filter(found -> found.getDbMark() == BaseEntity.VALID).orElse(null);
        if (message == null || !claimable(message)) {
            return Optional.empty();
        }
        message.setStatus(EmailStatus.SENDING);
        message.setAttempts(message.getAttempts() + 1);
        return Optional.of(emailMessageDAO.saveAndFlush(message));
    }

    public void markSent(Long id) {
        emailMessageDAO.findById(id).ifPresent(message -> {
            message.setStatus(EmailStatus.SENT);
            message.setSentTime(LocalDateTime.now());
            message.setLastError(null);
            emailMessageDAO.saveAndFlush(message);
        });
    }

    public void markFailed(Long id, String error) {
        emailMessageDAO.findById(id).ifPresent(message -> fail(message, error));
    }

    private boolean claimable(EmailMessage message) {
        return switch (message.getStatus()) {
            case PENDING -> true;
            case FAILED -> message.getAttempts() < maxAttempts;
            case SENDING -> {
                if (message.getTransTime().isAfter(LocalDateTime.now().minus(SENDING_TIMEOUT))) {
                    // 別的執行緒正在寄
                    yield false;
                }
                if (message.getAttempts() >= maxAttempts) {
                    fail(message, "寄送中斷，已達重試上限");
                    yield false;
                }
                yield true;
            }
            case SENT -> false;
        };
    }

    private void fail(EmailMessage message, String error) {
        message.setStatus(EmailStatus.FAILED);
        String text = error == null ? "未知錯誤" : error;
        message.setLastError(text.length() > 1000 ? text.substring(0, 1000) : text);
        emailMessageDAO.saveAndFlush(message);
    }
}
