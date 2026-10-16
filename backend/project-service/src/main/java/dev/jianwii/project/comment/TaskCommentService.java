package dev.jianwii.project.comment;

import dev.jianwii.api.project.TaskCommentQueryParameter;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskComment;
import dev.jianwii.project.activity.TaskActivityRecorder;
import dev.jianwii.project.project.ProjectRules;
import dev.jianwii.project.task.TaskDAO;
import org.springframework.stereotype.Service;

import java.util.Set;

/** 任務留言；只有作者能修改或刪除，新增時寫一筆活動紀錄。 */
@Service
public class TaskCommentService extends BaseService<TaskComment, TaskCommentQueryParameter> {
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final TaskActivityRecorder taskActivityRecorder;

    public TaskCommentService(TaskCommentDAO taskCommentDAO, TaskDAO taskDAO,
            ProjectRules projectRules, TaskActivityRecorder taskActivityRecorder) {
        super(taskCommentDAO);
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.taskActivityRecorder = taskActivityRecorder;
    }

    @Override
    public TaskComment create(TaskComment request) {
        TaskComment saved = super.create(request);
        taskActivityRecorder.commented(saved.getTaskId());
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
    private void requireOpenTask(Long taskId) {
        Task task = taskDAO.findById(taskId).filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new BusinessException("任務不存在：" + taskId));
        projectRules.requireOpen(task.getProjectId());
    }

    private static void requireAuthor(TaskComment comment) {
        if (!comment.getCreateId().equals(Operator.current())) {
            throw new ForbiddenException("只能修改或刪除自己的留言");
        }
    }
}
