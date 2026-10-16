package dev.jianwii.web.project;

import dev.jianwii.api.project.WorkLogApi;
import dev.jianwii.api.project.WorkLogQueryParameter;
import dev.jianwii.api.project.WorkLogSummary;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.WorkLog;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 前端用的工時 API，寫法同 {@link WebProjectController}；只能改、刪自己的工時由 project-service 檢查。 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/work-log")
public class WebWorkLogController {
    private final WorkLogApi workLogApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<WorkLog>> query(@Valid @RequestBody WorkLogQueryParameter parameter) {
        projectAccessGuard.scopeWorkLogs(parameter);
        return Result.success(workLogApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<WorkLog>> queryPage(
            @Valid @RequestBody WorkLogQueryParameter parameter) {
        projectAccessGuard.scopeWorkLogs(parameter);
        return Result.success(workLogApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('work-log:write')")
    @PostMapping
    public Result<WorkLog> create(
            @Validated(ValidGroup.Create.class) @RequestBody WorkLog workLog) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(workLog.getTaskId()));
        return Result.success(workLogApi.create(workLog));
    }

    @PreAuthorize("hasAuthority('work-log:write')")
    @PatchMapping("/{id}")
    public Result<WorkLog> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody WorkLog workLog) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfWorkLog(id));
        return Result.success(workLogApi.update(id, workLog));
    }

    @PreAuthorize("hasAuthority('work-log:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfWorkLog(id));
        workLogApi.delete(id);
        return Result.success();
    }

    /** 依人、日合計工時；範圍規則同查詢。 */
    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/summary")
    public Result<List<WorkLogSummary>> summary(
            @Valid @RequestBody WorkLogQueryParameter parameter) {
        projectAccessGuard.scopeWorkLogs(parameter);
        return Result.success(workLogApi.summary(parameter));
    }
}
