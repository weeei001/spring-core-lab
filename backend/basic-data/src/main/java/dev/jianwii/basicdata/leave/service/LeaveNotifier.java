package dev.jianwii.basicdata.leave.service;

import dev.jianwii.api.notification.NotificationApi;
import dev.jianwii.api.notification.NotificationSendRequest;
import dev.jianwii.entity.basicdata.leave.LeaveRequest;
import dev.jianwii.entity.basicdata.leave.LeaveStatus;
import dev.jianwii.entity.notification.NotificationType;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/** 沿用任務通知方式：commit 後發站內通知，通知服務失敗不回滾申請或審核。 */
@Component
@AllArgsConstructor
public class LeaveNotifier {
    private static final Logger log = LoggerFactory.getLogger(LeaveNotifier.class);
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationApi notificationApi;

    public void submitted(LeaveRequest request) {
        publish(request, request.getApproverId(), NotificationType.LEAVE_SUBMITTED, "有一筆請假申請待審核",
                "/leave/reviews");
    }

    public void reviewed(LeaveRequest request) {
        publish(request, request.getApplicantId(), NotificationType.LEAVE_REVIEWED,
                request.getStatus() == LeaveStatus.APPROVED ? "你的請假申請已核准" : "你的請假申請已退回",
                "/leave/requests");
    }

    private void publish(LeaveRequest request, String recipient, NotificationType type,
            String title, String path) {
        NotificationSendRequest notification = new NotificationSendRequest();
        notification.setRecipientIds(Set.of(recipient));
        notification.setType(type);
        notification.setTitle(title);
        // 原因與審核意見只在受權限保護的申請頁顯示。
        notification.setContent("申請 #" + request.getId() + "：" + request.getStartDate() + " 至 "
                + request.getEndDate());
        notification.setLink(path + "?request=" + request.getId());
        notification.setEmail(false);
        eventPublisher.publishEvent(new LeaveNotificationEvent(notification));
    }

    @TransactionalEventListener
    public void send(LeaveNotificationEvent event) {
        try {
            notificationApi.send(event.request());
        } catch (RuntimeException e) {
            log.warn("請假通知發送失敗：{}", event.request().getLink(), e);
        }
    }

    public record LeaveNotificationEvent(NotificationSendRequest request) {
    }
}
