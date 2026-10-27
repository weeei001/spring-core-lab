package dev.jianwii.api.basicdata.dto;

import dev.jianwii.entity.basicdata.leave.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** decision 只接受 APPROVED 或 REJECTED；退回時 comment 必填。 */
public record LeaveReview(
        @NotNull LeaveStatus decision, @Size(max = 1000) String comment) {}
