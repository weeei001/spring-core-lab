package dev.jianwii.web.project;

import dev.jianwii.api.project.MilestoneApi;
import dev.jianwii.api.project.MilestoneQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.Milestone;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 前端用的里程碑 API，寫法同 {@link WebProjectController}；只有專案經理能維護里程碑。 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/milestone")
public class WebMilestoneController {
    private final MilestoneApi milestoneApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<Milestone>> query(@Valid @RequestBody MilestoneQueryParameter parameter) {
        projectAccessGuard.scopeMilestones(parameter);
        return Result.success(milestoneApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<Milestone>> queryPage(
            @Valid @RequestBody MilestoneQueryParameter parameter) {
        projectAccessGuard.scopeMilestones(parameter);
        return Result.success(milestoneApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PostMapping
    public Result<Milestone> create(
            @Validated(ValidGroup.Create.class) @RequestBody Milestone milestone) {
        projectAccessGuard.requireManager(milestone.getProjectId());
        return Result.success(milestoneApi.create(milestone));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PatchMapping("/{id}")
    public Result<Milestone> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Milestone milestone) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfMilestone(id));
        return Result.success(milestoneApi.update(id, milestone));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfMilestone(id));
        milestoneApi.delete(id);
        return Result.success();
    }
}
