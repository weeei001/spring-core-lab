package dev.jianwii.project.service;

import dev.jianwii.api.notification.NotificationApi;
import dev.jianwii.api.notification.NotificationSendRequest;
import dev.jianwii.entity.notification.NotificationType;
import dev.jianwii.entity.project.ProjectStatus;
import dev.jianwii.entity.project.TaskStatus;
import dev.jianwii.project.dao.TaskDAO;
import dev.jianwii.project.dao.TaskDigestItem;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每天 09:00 起送摘要；同日重跑用通知唯一鍵去重，服務暫停後可補送。 */
@Component
@ConditionalOnProperty(name = "app.task-digest.enabled", havingValue = "true", matchIfMissing = true)
public class TaskDigestScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskDigestScheduler.class);
    private final TaskDAO taskDAO;
    private final NotificationApi notificationApi;
    private final ZoneId zone;
    private final boolean email;

    public TaskDigestScheduler(
            TaskDAO taskDAO,
            NotificationApi notificationApi,
            @Value("${app.task-digest.zone:Asia/Taipei}") String zone,
            @Value("${app.task-digest.email:false}") boolean email) {
        this.taskDAO = taskDAO;
        this.notificationApi = notificationApi;
        this.zone = ZoneId.of(zone);
        this.email = email;
    }

    @Scheduled(cron = "${app.task-digest.cron:0 */15 9-23 * * *}", zone = "${app.task-digest.zone:Asia/Taipei}")
    public void sendDailyDigest() {
        sendForDate(LocalDate.now(zone));
    }

    void sendForDate(LocalDate today) {
        Map<String, List<TaskDigestItem>> byUser = taskDAO
                .findDigestItems(
                        today.plusDays(3),
                        Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.REVIEW),
                        Set.of(ProjectStatus.PLANNING, ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD))
                .stream()
                .collect(Collectors.groupingBy(TaskDigestItem::assigneeId));
        byUser.forEach((userId, tasks) -> {
            try {
                NotificationSendRequest request = new NotificationSendRequest();
                request.setRecipientIds(Set.of(userId));
                request.setType(NotificationType.TASK_DAILY_DIGEST);
                request.setDedupKey("task-digest:" + today);
                request.setTitle(today + " 任務摘要");
                request.setContent(content(today, tasks));
                request.setLink("/my-tasks");
                request.setEmail(email);
                notificationApi.send(request);
            } catch (RuntimeException e) {
                // 一人的失敗不影響其他人，下次排程以相同鍵重試。
                log.warn("每日任務摘要發送失敗，userId：{}，日期：{}", userId, today, e);
            }
        });
    }

    static String content(LocalDate today, List<TaskDigestItem> tasks) {
        long overdue =
                tasks.stream().filter(task -> task.dueDate().isBefore(today)).count();
        StringBuilder text = new StringBuilder("逾期 ")
                .append(overdue)
                .append(" 件；今天至 ")
                .append(today.plusDays(3))
                .append(" 到期 ")
                .append(tasks.size() - overdue)
                .append(" 件");
        tasks.stream().limit(10).forEach(task -> {
            String title = task.title().length() > 80 ? task.title().substring(0, 80) + "…" : task.title();
            text.append("\n")
                    .append(task.dueDate())
                    .append(" · ")
                    .append(task.code())
                    .append(" ")
                    .append(title);
        });
        if (tasks.size() > 10) {
            text.append("\n其餘 ").append(tasks.size() - 10).append(" 件請至「我的任務」查看。");
        }
        return text.toString();
    }
}
