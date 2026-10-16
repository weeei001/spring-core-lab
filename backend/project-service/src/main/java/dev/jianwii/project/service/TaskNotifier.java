package dev.jianwii.project.service;

import dev.jianwii.api.notification.NotificationApi;
import dev.jianwii.api.notification.NotificationSendRequest;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.notification.NotificationType;
import dev.jianwii.entity.project.Project;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskComment;
import dev.jianwii.entity.project.TaskStatus;
import dev.jianwii.project.dao.ProjectDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 任務異動時通知相關的人（不通知操作者自己）：
 * <ul>
 * <li>指派：通知新的指派人，並寄信。</li>
 * <li>留言 @ 到：通知被 @ 的人，並寄信。</li>
 * <li>狀態變更、新留言：通知指派人與關注者，只發站內通知；已因 @ 收到通知的人不重複通知。</li>
 * </ul>
 * 由 Service 在交易內呼叫，但交易 commit 後才真的送到 notification-service：
 * 異動 rollback 就不會發出通知；notification-service 沒啟動或失敗只記 log，不影響任務本身。
 */
@Component
public class TaskNotifier {
    private static final Logger log = LoggerFactory.getLogger(TaskNotifier.class);
    private static final Map<TaskStatus, String> STATUS_LABELS = Map.of(TaskStatus.TODO, "待辦",
            TaskStatus.IN_PROGRESS, "進行中", TaskStatus.REVIEW, "審核中", TaskStatus.DONE, "完成",
            TaskStatus.CANCELLED, "取消");
    /** 留言內容在通知裡只顯示前幾個字。 */
    private static final int EXCERPT_LENGTH = 100;

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationApi notificationApi;
    private final ProjectDAO projectDAO;

    public TaskNotifier(ApplicationEventPublisher eventPublisher, NotificationApi notificationApi,
            ProjectDAO projectDAO) {
        this.eventPublisher = eventPublisher;
        this.notificationApi = notificationApi;
        this.projectDAO = projectDAO;
    }

    public void assigned(Task task) {
        publish(task, NotificationType.TASK_ASSIGNED, "指派給你：" + task.getTitle(),
                task.getDueDate() == null ? null : "到期日：" + task.getDueDate(), true,
                task.getAssigneeId());
    }

    public void statusChanged(Task task, TaskStatus from) {
        publish(task, NotificationType.TASK_STATUS_CHANGED, "任務狀態變更：" + task.getTitle(),
                "狀態：" + STATUS_LABELS.get(from) + " → " + STATUS_LABELS.get(task.getStatus()),
                false, followers(task, Set.of()));
    }

    /** 新留言：通知指派人與關注者；這次被 @ 的人另外收到 {@link #mentioned}，這裡排除。 */
    public void commented(Task task, TaskComment comment) {
        publish(task, NotificationType.TASK_COMMENTED, "新留言：" + task.getTitle(),
                excerpt(comment), false, followers(task, comment.getMentionIds()));
    }

    /** 留言 @ 到 userIds（只傳新被 @ 的人）。 */
    public void mentioned(Task task, TaskComment comment, Set<String> userIds) {
        publish(task, NotificationType.TASK_MENTIONED, "在留言中提到你：" + task.getTitle(),
                excerpt(comment), true, userIds.toArray(String[]::new));
    }

    /** 交易 commit 後才呼叫 notification-service；仍在原本的請求執行緒，通知的 createId 會是操作者。 */
    @TransactionalEventListener
    public void send(TaskNotificationEvent event) {
        try {
            notificationApi.send(event.request());
        } catch (RuntimeException e) {
            log.warn("通知發送失敗，標題：{}", event.request().getTitle(), e);
        }
    }

    private void publish(Task task, NotificationType type, String title, String detail,
            boolean email, String... recipientIds) {
        String operator = Operator.current();
        Set<String> recipients = new LinkedHashSet<>();
        Arrays.stream(recipientIds).filter(Objects::nonNull)
                .filter(id -> !id.equals(operator) && !id.equals(Operator.SYSTEM))
                .forEach(recipients::add);
        if (recipients.isEmpty()) {
            return;
        }
        NotificationSendRequest request = new NotificationSendRequest();
        request.setRecipientIds(recipients);
        request.setType(type);
        request.setTitle(truncate(title, 200));
        request.setContent(content(task, detail));
        request.setLink("/projects/" + task.getProjectId() + "?tab=tasks&task=" + task.getId());
        request.setEmail(email);
        eventPublisher.publishEvent(new TaskNotificationEvent(request));
    }

    /** 指派人與關注者，扣掉 excluded。 */
    private static String[] followers(Task task, Set<String> excluded) {
        Set<String> recipients = new LinkedHashSet<>();
        recipients.add(task.getAssigneeId());
        if (task.getWatcherIds() != null) {
            recipients.addAll(task.getWatcherIds());
        }
        recipients.removeAll(excluded == null ? Set.of() : excluded);
        return recipients.toArray(String[]::new);
    }

    private static String excerpt(TaskComment comment) {
        String content = comment.getContent();
        return content.length() > EXCERPT_LENGTH ? content.substring(0, EXCERPT_LENGTH) + "…"
                : content;
    }

    /** 專案名稱、任務編號，再接各通知自己的說明。 */
    private String content(Task task, String detail) {
        String project = projectDAO.findById(task.getProjectId()).map(Project::getName)
                .orElse(String.valueOf(task.getProjectId()));
        String content = "專案：" + project + "\n任務：" + task.getCode()
                + (detail == null ? "" : "\n" + detail);
        return truncate(content, 2000);
    }

    private static String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max - 1) + "…" : text;
    }

    /** 交易內暫存要發的通知，commit 後由 {@link #send} 送出。 */
    public record TaskNotificationEvent(NotificationSendRequest request) {
    }
}
