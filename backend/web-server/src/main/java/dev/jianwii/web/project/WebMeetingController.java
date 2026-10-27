package dev.jianwii.web.project;

import dev.jianwii.api.project.MeetingApi;
import dev.jianwii.api.project.MeetingQueryParameter;
import dev.jianwii.api.project.ProjectApi;
import dev.jianwii.api.project.ProjectQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.Meeting;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/meeting")
public class WebMeetingController {
    private final MeetingApi meetingApi;
    private final ProjectApi projectApi;
    private final ProjectAccessGuard projectAccessGuard;

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/query")
    public Result<List<Meeting>> query(@Valid @RequestBody MeetingQueryParameter parameter) {
        requireProjectMember(parameter.getProjectId());
        return Result.success(meetingApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<Meeting>> queryPage(@Valid @RequestBody MeetingQueryParameter parameter) {
        requireProjectMember(parameter.getProjectId());
        return Result.success(meetingApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/{id}")
    public Result<Meeting> get(@PathVariable Long id) {
        Meeting meeting = meetingApi.get(id);
        projectAccessGuard.requireMember(meeting.getProjectId());
        return Result.success(meeting);
    }

    @PreAuthorize("hasAuthority('meeting:write')")
    @PostMapping
    public Result<Meeting> create(@Validated(ValidGroup.Create.class) @RequestBody Meeting meeting) {
        requireProjectMember(meeting.getProjectId());
        return Result.success(meetingApi.create(meeting));
    }

    @PreAuthorize("hasAuthority('meeting:write')")
    @PatchMapping("/{id}")
    public Result<Meeting> update(
            @PathVariable Long id, @Validated(ValidGroup.Update.class) @RequestBody Meeting meeting) {
        projectAccessGuard.requireMeetingWriter(meetingApi.get(id));
        return Result.success(meetingApi.update(id, meeting));
    }

    private void requireProjectMember(Long projectId) {
        ProjectQueryParameter parameter = new ProjectQueryParameter();
        parameter.setIds(List.of(projectId));
        if (projectApi.query(parameter).isEmpty()) {
            throw new NotFoundException("查無專案");
        }
        projectAccessGuard.requireMember(projectId);
    }
}
