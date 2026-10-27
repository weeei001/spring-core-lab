package dev.jianwii.api.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MeetingTaskCreateRequest(
        @NotBlank @Size(max = 200) @Pattern(regexp = "(?sU).*\\S.*")
        String title,

        @Size(max = 100) String assigneeId,
        LocalDate dueDate) {}
