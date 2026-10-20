package dev.jianwii.web.notification;

import dev.jianwii.api.notification.NotificationApi;
import dev.jianwii.api.notification.NotificationQueryParameter;
import dev.jianwii.api.notification.NotificationSendRequest;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.web.Operator;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.notification.Notification;
import dev.jianwii.entity.notification.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 前端用的通知 API：呼叫 notification-service 的 {@link NotificationApi}，結果包成 Result。
 * 登入就能用，但只看得到、只能標記自己的通知；發公告需要 notification:send。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/notification")
public class WebNotificationController {
    private final NotificationApi notificationApi;

    @PostMapping("/query")
    public Result<List<Notification>> query(
            @Valid @RequestBody NotificationQueryParameter parameter) {
        parameter.setRecipientId(Operator.current());
        return Result.success(notificationApi.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<Notification>> queryPage(
            @Valid @RequestBody NotificationQueryParameter parameter) {
        parameter.setRecipientId(Operator.current());
        return Result.success(notificationApi.queryPage(parameter));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationApi.unreadCount());
    }

    @PostMapping("/{id}/read")
    public Result<Notification> markRead(@PathVariable Long id) {
        return Result.success(notificationApi.markRead(id));
    }

    @PostMapping("/read-all")
    public Result<Integer> markAllRead() {
        return Result.success(notificationApi.markAllRead());
    }

    /** 發公告給指定的使用者；種類固定為 ANNOUNCEMENT，其他種類只由系統依業務事件發出。 */
    @PreAuthorize("hasAuthority('notification:send')")
    @PostMapping("/announce")
    public Result<List<Notification>> announce(@Valid @RequestBody AnnouncementRequest request) {
        NotificationSendRequest send = new NotificationSendRequest();
        send.setRecipientIds(request.recipientIds());
        send.setType(NotificationType.ANNOUNCEMENT);
        send.setTitle(request.title());
        send.setContent(request.content());
        send.setEmail(request.email());
        return Result.success(notificationApi.send(send));
    }

    /** 公告內容；email 為 true 時同時寄信給有 Email 的收件人。 */
    public record AnnouncementRequest(@NotEmpty @Size(max = 100) Set<@NotBlank String> recipientIds,
            @NotBlank @Size(max = 200) String title, @Size(max = 2000) String content,
            boolean email) {
    }
}
