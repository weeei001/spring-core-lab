package dev.jianwii.web.project;

import dev.jianwii.api.project.ManualQaItemApi;
import dev.jianwii.api.project.ManualQaItemQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.ManualQaItem;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 前端用的驗收項目 API；已執行項目的鎖定規則由 project-service 檢查。 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/manual-qa-item")
public class WebManualQaItemController {
    private final ManualQaItemApi manualQaItemApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<ManualQaItem>> queryPage(@Valid @RequestBody ManualQaItemQueryParameter parameter) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaBatch(parameter.getBatchId()));
        return Result.success(manualQaItemApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/{id}")
    public Result<ManualQaItem> get(@PathVariable Long id) {
        ManualQaItem item = manualQaItemApi.get(id);
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaBatch(item.getBatchId()));
        return Result.success(item);
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping
    public Result<ManualQaItem> create(@Validated(ValidGroup.Create.class) @RequestBody ManualQaItem item) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaBatch(item.getBatchId()));
        return Result.success(manualQaItemApi.create(item));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PatchMapping("/{id}")
    public Result<ManualQaItem> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaItem item) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaItem(id));
        return Result.success(manualQaItemApi.update(id, item));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaItem(id));
        manualQaItemApi.delete(id);
        return Result.success();
    }
}
