package dev.jianwii.web.project;

import dev.jianwii.api.project.ProjectLabelApi;
import dev.jianwii.api.project.ProjectLabelQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.ProjectLabel;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 前端用的專案標籤 API，寫法同 {@link WebProjectController}；只有專案經理能維護標籤。 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/project-label")
public class WebProjectLabelController {
    private final ProjectLabelApi projectLabelApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<ProjectLabel>> query(@Valid @RequestBody ProjectLabelQueryParameter parameter) {
        projectAccessGuard.scopeLabels(parameter);
        return Result.success(projectLabelApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<ProjectLabel>> queryPage(@Valid @RequestBody ProjectLabelQueryParameter parameter) {
        projectAccessGuard.scopeLabels(parameter);
        return Result.success(projectLabelApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PostMapping
    public Result<ProjectLabel> create(@Validated(ValidGroup.Create.class) @RequestBody ProjectLabel label) {
        projectAccessGuard.requireManager(label.getProjectId());
        return Result.success(projectLabelApi.create(label));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PatchMapping("/{id}")
    public Result<ProjectLabel> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody ProjectLabel label) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfLabel(id));
        return Result.success(projectLabelApi.update(id, label));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfLabel(id));
        projectLabelApi.delete(id);
        return Result.success();
    }
}
