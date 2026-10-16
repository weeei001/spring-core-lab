package dev.jianwii.web.project;

import dev.jianwii.api.project.SprintApi;
import dev.jianwii.api.project.SprintQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.Sprint;
import dev.jianwii.entity.project.SprintBurndown;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的 Sprint API，寫法同 {@link WebProjectController}；只有專案經理能維護、開始、結束 Sprint。
 * 任務排入 Sprint 是修改任務（task:write），不在這裡。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/sprint")
public class WebSprintController {
    private final SprintApi sprintApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<Sprint>> query(@Valid @RequestBody SprintQueryParameter parameter) {
        projectAccessGuard.scopeSprints(parameter);
        return Result.success(sprintApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<Sprint>> queryPage(
            @Valid @RequestBody SprintQueryParameter parameter) {
        projectAccessGuard.scopeSprints(parameter);
        return Result.success(sprintApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PostMapping
    public Result<Sprint> create(@Validated(ValidGroup.Create.class) @RequestBody Sprint sprint) {
        projectAccessGuard.requireManager(sprint.getProjectId());
        return Result.success(sprintApi.create(sprint));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PatchMapping("/{id}")
    public Result<Sprint> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Sprint sprint) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfSprint(id));
        return Result.success(sprintApi.update(id, sprint));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfSprint(id));
        sprintApi.delete(id);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PostMapping("/{id}/start")
    public Result<Sprint> start(@PathVariable Long id) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfSprint(id));
        return Result.success(sprintApi.start(id));
    }

    /** 未完成的任務移到 moveToSprintId，不給則移回 Backlog。 */
    @PreAuthorize("hasAuthority('project:write')")
    @PostMapping("/{id}/close")
    public Result<Sprint> close(@PathVariable Long id,
            @RequestParam(required = false) Long moveToSprintId) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfSprint(id));
        return Result.success(sprintApi.close(id, moveToSprintId));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/{id}/burndown")
    public Result<List<SprintBurndown>> burndown(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfSprint(id));
        return Result.success(sprintApi.burndown(id));
    }
}
