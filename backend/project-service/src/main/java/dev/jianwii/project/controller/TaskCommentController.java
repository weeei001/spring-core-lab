package dev.jianwii.project.controller;

import dev.jianwii.api.project.api.TaskCommentApi;
import dev.jianwii.api.project.dto.TaskCommentQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.TaskComment;
import dev.jianwii.project.service.TaskCommentService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

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
