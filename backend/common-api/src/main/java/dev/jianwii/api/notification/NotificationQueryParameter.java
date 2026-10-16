package dev.jianwii.api.notification;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.notification.Notification;
import dev.jianwii.entity.notification.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 通知查詢條件；web-server 一律把 recipientId 設為登入者，只查得到自己的通知。 */
@Getter
@Setter
public class NotificationQueryParameter extends QueryParameter<Notification> {

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    @Size(max = 100)
    private String recipientId;

    private NotificationType type;

    /** true 只查未讀、false 只查已讀；條件為 readTime IS (NOT) NULL，由 NotificationService 組。 */
    @QueryIgnore
    private Boolean unread;
}
