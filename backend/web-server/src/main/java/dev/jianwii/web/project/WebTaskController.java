package dev.jianwii.web.project;

import dev.jianwii.api.project.api.TaskApi;
import dev.jianwii.api.project.dto.TaskQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskStatus;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 前端用的任務 API，寫法同 {@link WebProjectController}；專案成員都能維護任務。 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/task")
public class WebTaskController {
    private final TaskApi taskApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<Task>> query(@Valid @RequestBody TaskQueryParameter parameter) {
        projectAccessGuard.scopeTasks(parameter);
        return Result.success(taskApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<Task>> queryPage(@Valid @RequestBody TaskQueryParameter parameter) {
        projectAccessGuard.scopeTasks(parameter);
        return Result.success(taskApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping
    public Result<Task> create(@Validated(ValidGroup.Create.class) @RequestBody Task task) {
        projectAccessGuard.requireMember(task.getProjectId());
        return Result.success(taskApi.create(task));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PatchMapping("/{id}")
    public Result<Task> update(@PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody Task task) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(id));
        return Result.success(taskApi.update(id, task));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(id));
        taskApi.delete(id);
        return Result.success();
    }

    /** 看板拖曳：改狀態並排在 previousId 之後，previousId 不給表示排第一。 */
    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping("/{id}/move")
    public Result<Task> move(
            @PathVariable Long id, @RequestParam TaskStatus status, @RequestParam(required = false) Long previousId) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(id));
        return Result.success(taskApi.move(id, status, previousId));
    }

    /** 關注、取消關注只影響自己，有 project:read 的成員即可。 */
    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/{id}/watch")
    public Result<Task> watch(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(id));
        return Result.success(taskApi.watch(id));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @DeleteMapping("/{id}/watch")
    public Result<Task> unwatch(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(id));
        return Result.success(taskApi.unwatch(id));
    }
}
