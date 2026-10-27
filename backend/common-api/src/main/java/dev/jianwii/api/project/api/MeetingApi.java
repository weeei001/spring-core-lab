package dev.jianwii.api.project.api;

import dev.jianwii.api.project.dto.MeetingQueryParameter;
import dev.jianwii.api.project.dto.MeetingTaskCreateRequest;
import dev.jianwii.api.project.dto.MeetingTaskView;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.Meeting;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/api/meeting")
public interface MeetingApi {
    @PostExchange("/query")
    List<Meeting> query(@Valid @RequestBody MeetingQueryParameter parameter);

    @PostExchange("/queryPage")
    PageResponse<Meeting> queryPage(@Valid @RequestBody MeetingQueryParameter parameter);

    @GetExchange("/{id}")
    Meeting get(@PathVariable("id") Long id);

    @PostExchange
    Meeting create(@Validated(ValidGroup.Create.class) @RequestBody Meeting meeting);

    @PatchExchange("/{id}")
    Meeting update(@PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody Meeting meeting);

    @GetExchange("/{id}/tasks")
    List<MeetingTaskView> tasks(@PathVariable("id") Long id);

    @PostExchange("/{id}/tasks/{taskId}")
    Meeting linkTask(@PathVariable("id") Long id, @PathVariable("taskId") Long taskId);

    @DeleteExchange("/{id}/tasks/{taskId}")
    Meeting unlinkTask(@PathVariable("id") Long id, @PathVariable("taskId") Long taskId);

    @PostExchange("/{id}/tasks")
    MeetingTaskView createTask(@PathVariable("id") Long id, @Valid @RequestBody
    MeetingTaskCreateRequest request);
}
