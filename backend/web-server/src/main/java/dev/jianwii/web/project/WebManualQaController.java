package dev.jianwii.web.project;

import dev.jianwii.api.project.ManualQaApi;
import dev.jianwii.api.project.ManualQaApi.BatchInput;
import dev.jianwii.api.project.ManualQaApi.BatchView;
import dev.jianwii.api.project.ManualQaApi.BugExecutionView;
import dev.jianwii.api.project.ManualQaApi.ExecutionInput;
import dev.jianwii.api.project.ManualQaApi.ExecutionView;
import dev.jianwii.api.project.ManualQaApi.ItemInput;
import dev.jianwii.api.project.ManualQaApi.ItemView;
import dev.jianwii.api.project.ManualQaApi.Query;
import dev.jianwii.api.project.ProjectApi;
import dev.jianwii.api.project.ProjectQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.web.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/api")
public class WebManualQaController {
    private final ManualQaApi manualQaApi;
    private final ProjectApi projectApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/manual-qa-batch/queryPage")
    public Result<PageResponse<BatchView>> queryBatches(@Valid @RequestBody Query query) {
        requireProjectMember(query.projectId());
        return Result.success(manualQaApi.queryBatches(query));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/manual-qa-batch/{batchId}")
    public Result<BatchView> getBatch(@PathVariable Long batchId) {
        BatchView batch = manualQaApi.getBatch(batchId);
        projectAccessGuard.requireMember(batch.projectId());
        return Result.success(batch);
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping("/manual-qa-batch")
    public Result<BatchView> createBatch(@Valid @RequestBody BatchInput input) {
        requireProjectMember(input.projectId());
        return Result.success(manualQaApi.createBatch(input));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PatchMapping("/manual-qa-batch/{batchId}")
    public Result<BatchView> updateBatch(@PathVariable Long batchId, @Valid @RequestBody BatchInput input) {
        requireBatchMember(batchId);
        return Result.success(manualQaApi.updateBatch(batchId, input));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping("/manual-qa-batch/{batchId}/copy")
    public Result<BatchView> copyBatch(@PathVariable Long batchId, @Valid @RequestBody BatchInput input) {
        requireBatchMember(batchId);
        return Result.success(manualQaApi.copyBatch(batchId, input));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/manual-qa-batch/{batchId}/items")
    public Result<PageResponse<ItemView>> items(
            @PathVariable Long batchId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size) {
        requireBatchMember(batchId);
        return Result.success(manualQaApi.items(batchId, page, size));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping("/manual-qa-batch/{batchId}/items")
    public Result<ItemView> createItem(@PathVariable Long batchId, @Valid @RequestBody ItemInput input) {
        requireBatchMember(batchId);
        return Result.success(manualQaApi.createItem(batchId, input));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PatchMapping("/manual-qa-item/{itemId}")
    public Result<ItemView> updateItem(@PathVariable Long itemId, @RequestBody Map<String, Object> input) {
        requireItemMember(itemId);
        return Result.success(manualQaApi.updateItem(itemId, input));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @DeleteMapping("/manual-qa-item/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        requireItemMember(itemId);
        manualQaApi.deleteItem(itemId);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/manual-qa-item/{itemId}/executions")
    public Result<PageResponse<ExecutionView>> executions(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size) {
        requireItemMember(itemId);
        return Result.success(manualQaApi.executions(itemId, page, size));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PostMapping("/manual-qa-item/{itemId}/executions")
    public Result<ExecutionView> createExecution(@PathVariable Long itemId, @Valid @RequestBody ExecutionInput input) {
        requireItemMember(itemId);
        return Result.success(manualQaApi.createExecution(itemId, input));
    }

    @PreAuthorize("hasAuthority('task:write')")
    @PatchMapping("/manual-qa-execution/{executionId}/bug")
    public Result<ExecutionView> updateExecutionBug(
            @PathVariable Long executionId, @RequestBody Map<String, Object> input) {
        requireItemMember(manualQaApi.getExecution(executionId).itemId());
        return Result.success(manualQaApi.updateExecutionBug(executionId, input));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/manual-qa-bug/{taskId}/executions")
    public Result<PageResponse<BugExecutionView>> bugExecutions(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(taskId));
        return Result.success(manualQaApi.bugExecutions(taskId, page, size));
    }

    private void requireProjectMember(Long projectId) {
        if (projectId == null) {
            throw new BusinessException("請指定 projectId");
        }
        ProjectQueryParameter parameter = new ProjectQueryParameter();
        parameter.setIds(List.of(projectId));
        if (projectApi.query(parameter).isEmpty()) {
            throw new NotFoundException("查無專案");
        }
        projectAccessGuard.requireMember(projectId);
    }

    private void requireBatchMember(Long batchId) {
        projectAccessGuard.requireMember(manualQaApi.getBatch(batchId).projectId());
    }

    private void requireItemMember(Long itemId) {
        requireBatchMember(manualQaApi.getItem(itemId).batchId());
    }
}
