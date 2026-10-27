package dev.jianwii.web.project;

import dev.jianwii.api.project.api.ManualQaExecutionApi;
import dev.jianwii.api.project.dto.ManualQaBugSource;
import dev.jianwii.api.project.dto.ManualQaExecutionQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.ManualQaExecution;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 前端用的驗收紀錄 API；執行人由 project-service 依登入者填入。 */
@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/api/manual-qa-execution")
public class WebManualQaExecutionController {
    private final ManualQaExecutionApi manualQaExecutionApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<ManualQaExecution>> queryPage(
            @Valid @RequestBody ManualQaExecutionQueryParameter parameter) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaItem(parameter.getItemId()));
        return Result.success(manualQaExecutionApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('manual-qa:write')")
    @PostMapping
    public Result<ManualQaExecution> create(
            @Validated(ValidGroup.Create.class) @RequestBody ManualQaExecution execution) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaItem(execution.getItemId()));
        return Result.success(manualQaExecutionApi.create(execution));
    }

    @PreAuthorize("hasAuthority('manual-qa:write')")
    @PatchMapping("/{id}")
    public Result<ManualQaExecution> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaExecution execution) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfQaExecution(id));
        return Result.success(manualQaExecutionApi.update(id, execution));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/bug/{taskId}")
    public Result<PageResponse<ManualQaBugSource>> bugSources(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(taskId));
        return Result.success(manualQaExecutionApi.bugSources(taskId, page, size));
    }
}
