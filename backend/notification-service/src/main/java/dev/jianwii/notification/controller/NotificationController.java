package dev.jianwii.notification.controller;

import dev.jianwii.api.notification.NotificationApi;
import dev.jianwii.api.notification.NotificationQueryParameter;
import dev.jianwii.api.notification.NotificationSendRequest;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.notification.Notification;
import dev.jianwii.notification.service.NotificationSender;
import dev.jianwii.notification.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link NotificationApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 * 「我的」通知以 {@link Operator#current()} 為準。
 */
@RestController
@AllArgsConstructor
public class NotificationController implements NotificationApi {
    private final NotificationService notificationService;
    private final NotificationSender notificationSender;

    @Override
    public List<Notification> query(NotificationQueryParameter parameter) {
        return notificationService.query(parameter);
    }

    @Override
    public PageResponse<Notification> queryPage(NotificationQueryParameter parameter) {
        return notificationService.queryPage(parameter);
    }

    @Override
    public List<Notification> send(NotificationSendRequest request) {
        return notificationSender.send(request);
    }

    @Override
    public long unreadCount() {
        return notificationService.unreadCount(Operator.current());
    }

    @Override
    public Notification markRead(Long id) {
        return notificationService.markRead(id, Operator.current());
    }

    @Override
    public int markAllRead() {
        return notificationService.markAllRead(Operator.current());
    }
}
