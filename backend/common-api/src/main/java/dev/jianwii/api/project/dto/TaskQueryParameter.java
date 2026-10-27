package dev.jianwii.api.project.dto;

import dev.jianwii.common.query.QueryConditions;
import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskPriority;
import dev.jianwii.entity.project.TaskStatus;
import dev.jianwii.entity.project.TaskType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 任務查詢條件，寫法說明見 {@link QueryParameter}。 沒有 project:admin 的人：給 projectId 時必須是該專案成員；只給 ids
 * 時這些任務的專案都要是自己參與的；都沒給時 web-server 會把 assigneeId 設為登入者（我的任務）。
 */
@Getter
@Setter
public class TaskQueryParameter extends QueryParameter<Task> {

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    private Long projectId;

    /** web-server 強制設定，查詢時以目前有效的成員關係限制資料。 */
    @QueryIgnore
    @Size(max = 100)
    private String memberUserId;

    /** 查某任務的子任務。 */
    private Long parentId;

    /** true：只查主任務（parentId 為 null）；false：只查子任務。由 TaskService.toSpecification 處理。 */
    @QueryIgnore
    private Boolean topLevel;

    @QueryField(property = "status", operator = QueryOperator.IN)
    @Size(max = 10)
    private List<@NotNull TaskStatus> statuses;

    @QueryField(property = "priority", operator = QueryOperator.IN)
    @Size(max = 10)
    private List<@NotNull TaskPriority> priorities;

    @QueryField(property = "type", operator = QueryOperator.IN)
    @Size(max = 10)
    private List<@NotNull TaskType> types;

    /** 掛了任一個標籤即符合；標籤在中間表，由 TaskService.toSpecification 處理。 */
    @QueryIgnore
    @Size(max = 20)
    private List<@NotNull Long> labelIds;

    @Size(max = 100)
    private String assigneeId;

    /** code 或 title 任一包含關鍵字；不可全為空白。 */
    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;

    /** 到期日區間 [fromDueDate, toDueDate]；逾期 = toDueDate 為昨天 + 未完成的狀態。 */
    @QueryField(property = "dueDate", operator = QueryOperator.GREATER_THAN_OR_EQUAL)
    private LocalDate fromDueDate;

    @QueryField(property = "dueDate", operator = QueryOperator.LESS_THAN_OR_EQUAL)
    private LocalDate toDueDate;

    @Override
    public QueryConditions toQueryConditions() {
        return super.toQueryConditions().keyword(keyword, "code", "title");
    }

    @AssertTrue(message = "fromDueDate 不可晚於 toDueDate")
    private boolean isDueDateRangeValid() {
        return fromDueDate == null || toDueDate == null || !fromDueDate.isAfter(toDueDate);
    }
}
