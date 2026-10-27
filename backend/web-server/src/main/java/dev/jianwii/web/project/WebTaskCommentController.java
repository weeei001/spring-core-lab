package dev.jianwii.web.project;

import dev.jianwii.api.project.api.TaskCommentApi;
import dev.jianwii.api.project.dto.TaskCommentQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.TaskComment;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 前端用的任務留言 API，寫法同 {@link WebProjectController}；只能改、刪自己的留言由 project-service 檢查。 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/task-comment")
public class WebTaskCommentController {
    private final TaskCommentApi taskCommentApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<TaskComment>> query(@Valid @RequestBody TaskCommentQueryParameter parameter) {
        projectAccessGuard.scopeComments(parameter);
        return Result.success(taskCommentApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<TaskComment>> queryPage(@Valid @RequestBody TaskCommentQueryParameter parameter) {
        projectAccessGuard.scopeComments(parameter);
        return Result.success(taskCommentApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping
    public Result<TaskComment> create(@Validated(ValidGroup.Create.class) @RequestBody TaskComment comment) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(comment.getTaskId()));
        return Result.success(taskCommentApi.create(comment));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PatchMapping("/{id}")
    public Result<TaskComment> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody TaskComment comment) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfComment(id));
        return Result.success(taskCommentApi.update(id, comment));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfComment(id));
        taskCommentApi.delete(id);
        return Result.success();
    }
}
