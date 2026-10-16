package dev.jianwii.entity.notification;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 待寄／已寄的 Email（outbox）。和通知在同一個交易寫入，commit 後才寄，交易 rollback 就不會寄出；
 * 寄信失敗留在資料表等排程重試，服務重啟也不會遺失。只在 notification-service 內部使用，沒有對外 API。
 */
@Entity
@Table(name = "email_message", indexes = @Index(columnList = "status, trans_time"))
@Getter
@Setter
public class EmailMessage extends BaseEntity {

    /** 對應的站內通知；選填。 */
    @Column(name = "notification_id", updatable = false)
    private Long notificationId;

    @Column(name = "recipient", nullable = false, updatable = false, length = 254)
    private String recipient;

    @Column(name = "subject", nullable = false, updatable = false, length = 300)
    private String subject;

    @Column(name = "body", nullable = false, updatable = false, length = 5000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status;

    /** 已嘗試寄出的次數。 */
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    /** 最後一次失敗的原因。 */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "sent_time", columnDefinition = "datetime(6)")
    private LocalDateTime sentTime;

    /**
     * 樂觀鎖：領取（PENDING → SENDING）時版本不符代表別的執行緒先領走了，自己就放棄，避免同一封信寄兩次。
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
