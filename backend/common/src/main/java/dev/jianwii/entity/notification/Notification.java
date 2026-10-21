package dev.jianwii.entity.notification;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 站內通知，一位收件人一筆；由 notification-service 依 NotificationApi.send 建立，只能查詢與標記已讀。 觸發的人即 createId（系統觸發為
 * system），時間即 createTime。
 */
@Entity
@Table(
        name = "notification",
        indexes = @Index(columnList = "recipient_id, read_time"),
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_notification_recipient_dedup",
                        columnNames = {"recipient_id", "dedup_key"}))
@Getter
@Setter
public class Notification extends BaseEntity {

    /** 收件人的 user id。 */
    @Column(name = "recipient_id", nullable = false, updatable = false, length = 100)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, updatable = false, length = 200)
    private String title;

    @Column(name = "content", updatable = false, length = 2000)
    private String content;

    /** 點通知要前往的前端路徑，例如 /projects/1?tab=tasks&task=2；一定是 / 開頭的站內路徑。 */
    @Column(name = "link", updatable = false, length = 500)
    private String link;

    /** 已讀時間；null 為未讀。 */
    @Column(name = "read_time", columnDefinition = "datetime(6)")
    private LocalDateTime readTime;

    @Column(name = "dedup_key", updatable = false, length = 100)
    private String dedupKey;
}
