package dev.jianwii.project.controller;

import dev.jianwii.api.project.api.TaskApi;
import dev.jianwii.api.project.dto.TaskQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskStatus;
import dev.jianwii.project.service.TaskService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路徑、參數與驗證註解都寫在 {@link TaskApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class TaskController implements TaskApi {
    private final TaskService taskService;

    @Override
    public List<Task> query(TaskQueryParameter parameter) {
        return taskService.query(parameter);
    }

    @Override
    public PageResponse<Task> queryPage(TaskQueryParameter parameter) {
        return taskService.queryPage(parameter);
    }

    @Override
    public Task create(Task task) {
        return taskService.create(task);
    }

    @Override
    public Task update(Long id, Task task) {
        return taskService.update(id, task);
    }

    @Override
    public void delete(Long id) {
        taskService.delete(id);
    }

    @Override
    public Task move(Long id, TaskStatus status, Long previousId) {
        return taskService.move(id, status, previousId);
    }

    @Override
    public Task watch(Long id) {
        return taskService.watch(id);
    }

    @Override
    public Task unwatch(Long id) {
        return taskService.unwatch(id);
    }
}
