package dev.jianwii.web.project;

import dev.jianwii.api.project.api.TaskActivityApi;
import dev.jianwii.api.project.dto.TaskActivityQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.TaskActivity;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 前端用的任務活動紀錄 API（只能查詢），寫法同 {@link WebProjectController}。 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/task-activity")
public class WebTaskActivityController {
    private final TaskActivityApi taskActivityApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<TaskActivity>> query(@Valid @RequestBody TaskActivityQueryParameter parameter) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(parameter.getTaskId()));
        return Result.success(taskActivityApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<TaskActivity>> queryPage(@Valid @RequestBody TaskActivityQueryParameter parameter) {
        projectAccessGuard.requireMember(projectAccessGuard.projectOfTask(parameter.getTaskId()));
        return Result.success(taskActivityApi.queryPage(parameter));
    }
}
