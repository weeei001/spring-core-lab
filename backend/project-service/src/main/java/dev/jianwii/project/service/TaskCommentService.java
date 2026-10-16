package dev.jianwii.project.service;

import dev.jianwii.api.project.TaskCommentQueryParameter;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskComment;
import dev.jianwii.project.dao.TaskCommentDAO;
import dev.jianwii.project.dao.TaskDAO;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 任務留言；只有作者能修改或刪除。新增時寫一筆活動紀錄、通知指派人與關注者，
 * 被 @ 的人另外收到通知；留言者與被 @ 的人自動關注任務。
 */
@Service
public class TaskCommentService extends BaseService<TaskComment, TaskCommentQueryParameter> {
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final TaskActivityRecorder taskActivityRecorder;
    private final TaskNotifier taskNotifier;

    public TaskCommentService(TaskCommentDAO taskCommentDAO, TaskDAO taskDAO,
            ProjectRules projectRules, TaskActivityRecorder taskActivityRecorder,
            TaskNotifier taskNotifier) {
        super(taskCommentDAO);
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.taskActivityRecorder = taskActivityRecorder;
        this.taskNotifier = taskNotifier;
    }

    @Override
    public TaskComment create(TaskComment request) {
        TaskComment saved = super.create(request);
        Task task = requireOpenTask(saved.getTaskId());
        TaskService.addWatcher(task, saved.getCreateId());
        saved.getMentionIds().forEach(userId -> TaskService.addWatcher(task, userId));
        taskDAO.save(task);
        taskActivityRecorder.commented(saved.getTaskId());
        taskNotifier.commented(task, saved);
        if (!saved.getMentionIds().isEmpty()) {
            taskNotifier.mentioned(task, saved, saved.getMentionIds());
        }
        return saved;
    }

    /** 修改留言時新 @ 到的人才通知，原本就 @ 過的不再通知。 */
    @Override
    public TaskComment update(Long id, TaskComment request) {
        Set<String> before = Set.copyOf(find(id).getMentionIds());
        TaskComment saved = super.update(id, request);
        Set<String> added = new LinkedHashSet<>(saved.getMentionIds());
        added.removeAll(before);
        if (!added.isEmpty()) {
            Task task = requireOpenTask(saved.getTaskId());
            added.forEach(userId -> TaskService.addWatcher(task, userId));
            taskDAO.save(task);
            taskNotifier.mentioned(task, saved, added);
        }
        return saved;
    }

    @Override
    protected void beforeCreate(TaskComment request) {
        Task task = requireOpenTask(request.getTaskId());
        if (request.getMentionIds() == null) {
            request.setMentionIds(new HashSet<>());
        }
        requireMembers(task.getProjectId(), request.getMentionIds());
    }

    @Override
    protected void beforeUpdate(TaskComment entity, TaskComment request) {
        request.setTaskId(null);
        requireAuthor(entity);
        Task task = requireOpenTask(entity.getTaskId());
        if (request.getMentionIds() != null) {
            requireMembers(task.getProjectId(), request.getMentionIds());
            // 只改 @ 的人時主表不會 UPDATE，強制更新修改者與時間
            touch(entity);
        }
    }

    @Override
    protected void beforeDelete(TaskComment entity) {
        requireAuthor(entity);
        requireOpenTask(entity.getTaskId());
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime");
    }

    /** 回傳前載入 @ 到的人（LAZY 的中間表）。 */
    @Override
    protected void initialize(TaskComment comment) {
        Hibernate.initialize(comment.getMentionIds());
    }

    private void requireMembers(Long projectId, Set<String> userIds) {
        userIds.forEach(userId -> projectRules.requireMember(projectId, userId));
    }

    /** 任務必須有效，且所屬專案未結案。 */
    private Task requireOpenTask(Long taskId) {
        Task task = taskDAO.findById(taskId).filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new BusinessException("任務不存在：" + taskId));
        projectRules.requireOpen(task.getProjectId());
        return task;
    }

    private static void requireAuthor(TaskComment comment) {
        if (!comment.getCreateId().equals(Operator.current())) {
            throw new ForbiddenException("只能修改或刪除自己的留言");
        }
    }
}
