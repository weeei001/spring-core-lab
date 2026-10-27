package dev.jianwii.api.notification.api;

import dev.jianwii.api.notification.dto.NotificationQueryParameter;
import dev.jianwii.api.notification.dto.NotificationSendRequest;
import dev.jianwii.api.project.api.ProjectApi;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.notification.Notification;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * notification-service 的通知 API，用法同 {@link ProjectApi}。 通知由 {@link #send}
 * 建立，不能修改內容或刪除；「我的」指 {@code X-Operator} header 的使用者。
 */
@HttpExchange("/api/notification")
public interface NotificationApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<Notification> query(@Valid @RequestBody NotificationQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<Notification> queryPage(@Valid @RequestBody NotificationQueryParameter parameter);

    /** 發送通知，回傳建立的通知（每位收件人一筆）；Email 在交易 commit 後才非同步寄出。 */
    @PostExchange("/send")
    List<Notification> send(@Valid @RequestBody NotificationSendRequest request);

    /** 我的未讀數。 */
    @GetExchange("/unread-count")
    long unreadCount();

    /** 標記為已讀；只能標記自己的通知，已讀的不變。 */
    @PostExchange("/{id}/read")
    Notification markRead(@PathVariable("id") Long id);

    /** 把我的通知全部標記為已讀，回傳這次標記的筆數。 */
    @PostExchange("/read-all")
    int markAllRead();
}
