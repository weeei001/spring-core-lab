package dev.jianwii.web.project;

import dev.jianwii.api.project.ProjectApi;
import dev.jianwii.api.project.ProjectQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.Project;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 前端用的專案 API：檢查功能權限（@PreAuthorize）與成員資格（{@link ProjectAccessGuard}）， 再呼叫 project-service 的
 * {@link ProjectApi}，結果包成 Result。 後端回的錯誤會變成 RemoteServiceException，再由 ExceptionHandleController
 * 轉成同樣的 Result.failure 回前端。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/project")
public class WebProjectController {
    private final ProjectApi projectApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<Project>> query(@Valid @RequestBody ProjectQueryParameter parameter) {
        projectAccessGuard.scopeProjects(parameter);
        return Result.success(projectApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<Project>> queryPage(@Valid @RequestBody ProjectQueryParameter parameter) {
        projectAccessGuard.scopeProjects(parameter);
        return Result.success(projectApi.queryPage(parameter));
    }

    /** 建立者自動成為專案經理。 */
    @PreAuthorize("hasAuthority('project:write')")
    @PostMapping
    public Result<Project> create(@Validated(ValidGroup.Create.class) @RequestBody Project project) {
        return Result.success(projectApi.create(project));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PatchMapping("/{id}")
    public Result<Project> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody Project project) {
        projectAccessGuard.requireManager(id);
        return Result.success(projectApi.update(id, project));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireManager(id);
        projectApi.delete(id);
        return Result.success();
    }
}
