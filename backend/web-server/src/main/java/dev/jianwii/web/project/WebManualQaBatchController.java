package dev.jianwii.web.project;

import dev.jianwii.api.project.api.ManualQaBatchApi;
import dev.jianwii.api.project.dto.ManualQaBatchQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.ManualQaBatch;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 前端用的驗收批次 API；寫入沿用 task:write，結案唯讀由 project-service 檢查。 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/manual-qa-batch")
public class WebManualQaBatchController {
    private final ManualQaBatchApi manualQaBatchApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<ManualQaBatch>> queryPage(@Valid @RequestBody ManualQaBatchQueryParameter parameter) {
        projectAccessGuard.requireMember(parameter.getProjectId());
        return Result.success(manualQaBatchApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/{id}")
    public Result<ManualQaBatch> get(@PathVariable Long id) {
        ManualQaBatch batch = manualQaBatchApi.get(id);
        projectAccessGuard.requireMember(batch.getProjectId());
        return Result.success(batch);
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping
    public Result<ManualQaBatch> create(@Validated(ValidGroup.Create.class) @RequestBody ManualQaBatch batch) {
        projectAccessGuard.requireMember(batch.getProjectId());
        return Result.success(manualQaBatchApi.create(batch));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PatchMapping("/{id}")
    public Result<ManualQaBatch> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaBatch batch) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaBatch(id));
        return Result.success(manualQaBatchApi.update(id, batch));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping("/{id}/copy")
    public Result<ManualQaBatch> copy(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaBatch batch) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaBatch(id));
        return Result.success(manualQaBatchApi.copy(id, batch));
    }
}
