package dev.jianwii.project.comment;

import dev.jianwii.api.project.TaskCommentApi;
import dev.jianwii.api.project.TaskCommentQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.TaskComment;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link TaskCommentApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class TaskCommentController implements TaskCommentApi {
    private final TaskCommentService taskCommentService;

    @Override
    public List<TaskComment> query(TaskCommentQueryParameter parameter) {
        return taskCommentService.query(parameter);
    }

    @Override
    public PageResponse<TaskComment> queryPage(TaskCommentQueryParameter parameter) {
        return taskCommentService.queryPage(parameter);
    }

    @Override
    public TaskComment create(TaskComment comment) {
        return taskCommentService.create(comment);
    }

    @Override
    public TaskComment update(Long id, TaskComment comment) {
        return taskCommentService.update(id, comment);
    }

    @Override
    public void delete(Long id) {
        taskCommentService.delete(id);
    }
}
