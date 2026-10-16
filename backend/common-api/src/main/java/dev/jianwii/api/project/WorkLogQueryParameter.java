package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.WorkLog;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 工時查詢條件，寫法說明見 {@link QueryParameter}。
 * 沒有 project:admin 的人：給 projectId 或 taskId 時必須是該專案成員；都沒給時 web-server 會把 userId 設為登入者（我的工時）。
 */
@Getter
@Setter
public class WorkLogQueryParameter extends QueryParameter<WorkLog> {

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    private Long projectId;

    private Long taskId;

    @Size(max = 100)
    private String userId;

    /** 工作日期區間 [fromDate, toDate]。 */
    @QueryField(property = "workDate", operator = QueryOperator.GREATER_THAN_OR_EQUAL)
    private LocalDate fromDate;

    @QueryField(property = "workDate", operator = QueryOperator.LESS_THAN_OR_EQUAL)
    private LocalDate toDate;

    @AssertTrue(message = "fromDate 不可晚於 toDate")
    private boolean isDateRangeValid() {
        return fromDate == null || toDate == null || !fromDate.isAfter(toDate);
    }
}
