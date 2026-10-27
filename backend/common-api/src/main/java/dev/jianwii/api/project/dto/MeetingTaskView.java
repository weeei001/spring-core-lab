package dev.jianwii.api.project.dto;

import dev.jianwii.entity.project.TaskStatus;
import java.time.LocalDate;

public record MeetingTaskView(
        Long taskId,
        boolean deleted,
        String code,
        String title,
        String assigneeId,
        LocalDate dueDate,
        TaskStatus status) {}
