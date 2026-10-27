package dev.jianwii.project.controller;

import dev.jianwii.api.project.api.MeetingApi;
import dev.jianwii.api.project.dto.MeetingQueryParameter;
import dev.jianwii.api.project.dto.MeetingTaskCreateRequest;
import dev.jianwii.api.project.dto.MeetingTaskView;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.Meeting;
import dev.jianwii.project.service.MeetingService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class MeetingController implements MeetingApi {
    private final MeetingService meetingService;

    @Override
    public List<Meeting> query(MeetingQueryParameter parameter) {
        return meetingService.query(parameter);
    }

    @Override
    public PageResponse<Meeting> queryPage(MeetingQueryParameter parameter) {
        return meetingService.queryPage(parameter);
    }

    @Override
    public Meeting get(Long id) {
        return meetingService.get(id);
    }

    @Override
    public Meeting create(Meeting meeting) {
        return meetingService.create(meeting);
    }

    @Override
    public Meeting update(Long id, Meeting meeting) {
        return meetingService.update(id, meeting);
    }

    @Override
    public List<MeetingTaskView> tasks(Long id) {
        return meetingService.tasks(id);
    }

    @Override
    public Meeting linkTask(Long id, Long taskId) {
        return meetingService.linkTask(id, taskId);
    }

    @Override
    public Meeting unlinkTask(Long id, Long taskId) {
        return meetingService.unlinkTask(id, taskId);
    }

    @Override
    public MeetingTaskView createTask(Long id, MeetingTaskCreateRequest request) {
        return meetingService.createTask(id, request);
    }
}
