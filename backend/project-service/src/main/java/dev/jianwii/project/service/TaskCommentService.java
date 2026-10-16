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
import org.springframework.stereotype.Service;

import java.util.Set;

/** 任務留言；只有作者能修改或刪除，新增時寫一筆活動紀錄並通知任務的指派人與建立者。 */
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
        taskActivityRecorder.commented(saved.getTaskId());
        taskNotifier.commented(requireOpenTask(saved.getTaskId()), saved);
        return saved;
    }

    @Override
    protected void beforeCreate(TaskComment request) {
        requireOpenTask(request.getTaskId());
    }

    @Override
    protected void beforeUpdate(TaskComment entity, TaskComment request) {
        request.setTaskId(null);
        requireAuthor(entity);
        requireOpenTask(entity.getTaskId());
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
