package dev.jianwii.api.project.dto;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.Meeting;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeetingQueryParameter extends QueryParameter<Meeting> {
    @NotNull
    private Long projectId;

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(min = 1, max = 100)
    private List<@NotNull Long> ids;

    @QueryIgnore
    private Long taskId;

    @QueryField(property = "heldAt", operator = QueryOperator.GREATER_THAN_OR_EQUAL)
    private LocalDateTime fromHeldAt;

    @QueryField(property = "heldAt", operator = QueryOperator.LESS_THAN_OR_EQUAL)
    private LocalDateTime toHeldAt;

    @AssertTrue(message = "fromHeldAt 不可晚於 toHeldAt")
    private boolean isHeldAtRangeValid() {
        return fromHeldAt == null || toHeldAt == null || !fromHeldAt.isAfter(toHeldAt);
    }
}
