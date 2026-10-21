package dev.jianwii.api.basicdata.leave;

import dev.jianwii.entity.basicdata.leave.LeaveType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 不接受申請人、狀態或審核欄位，避免呼叫端偽造。 */
public record LeaveSubmission(
        @NotNull LeaveType type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank @Size(max = 1000) String reason,

        @NotBlank @Pattern(regexp = "[1-9]\\d{0,18}") @DecimalMax("9223372036854775807")
        String approverId) {}
