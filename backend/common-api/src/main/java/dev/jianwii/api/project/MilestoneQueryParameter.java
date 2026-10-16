package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.Milestone;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 里程碑查詢條件，寫法說明見 {@link QueryParameter}。
 * 沒有 project:admin 的人，web-server 會要求給 projectId 或 projectIds，且都必須是自己參與的專案。
 */
@Getter
@Setter
public class MilestoneQueryParameter extends QueryParameter<Milestone> {

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    private Long projectId;

    /** IN：例如儀表板查自己所有專案即將到期的里程碑。 */
    @QueryField(property = "projectId", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> projectIds;

    private Boolean achieved;

    /** 到期日區間 [fromDueDate, toDueDate]。 */
    @QueryField(property = "dueDate", operator = QueryOperator.GREATER_THAN_OR_EQUAL)
    private LocalDate fromDueDate;

    @QueryField(property = "dueDate", operator = QueryOperator.LESS_THAN_OR_EQUAL)
    private LocalDate toDueDate;

    @AssertTrue(message = "fromDueDate 不可晚於 toDueDate")
    private boolean isDueDateRangeValid() {
        return fromDueDate == null || toDueDate == null || !fromDueDate.isAfter(toDueDate);
    }
}
