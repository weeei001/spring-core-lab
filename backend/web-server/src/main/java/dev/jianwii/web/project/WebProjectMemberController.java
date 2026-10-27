package dev.jianwii.web.project;

import dev.jianwii.api.project.api.ProjectMemberApi;
import dev.jianwii.api.project.dto.ProjectMemberQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.ProjectMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 前端用的專案成員 API，寫法同 {@link WebProjectController}；只有專案經理能維護成員。 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/project-member")
public class WebProjectMemberController {
    private final ProjectMemberApi projectMemberApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<ProjectMember>> query(@Valid @RequestBody ProjectMemberQueryParameter parameter) {
        projectAccessGuard.scopeMembers(parameter);
        return Result.success(projectMemberApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<ProjectMember>> queryPage(@Valid @RequestBody ProjectMemberQueryParameter parameter) {
        projectAccessGuard.scopeMembers(parameter);
        return Result.success(projectMemberApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PostMapping
    public Result<ProjectMember> create(@Validated(ValidGroup.Create.class) @RequestBody ProjectMember member) {
        projectAccessGuard.requireManager(member.getProjectId());
        return Result.success(projectMemberApi.create(member));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @PatchMapping("/{id}")
    public Result<ProjectMember> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody ProjectMember member) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfMember(id));
        return Result.success(projectMemberApi.update(id, member));
    }

    @PreAuthorize("hasAuthority('project:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireManager(projectAccessGuard.projectOfMember(id));
        projectMemberApi.delete(id);
        return Result.success();
    }
}
