package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * 工時：成員在某天花在某任務的時數。只能登記、修改、刪除自己的工時。 projectId 由任務帶入（冗餘存放），統計專案工時時不必 join task。
 */
@Entity
@Table(
        name = "work_log",
        indexes = {
            @Index(columnList = "project_id, work_date"),
            @Index(columnList = "user_id, work_date"),
            @Index(columnList = "task_id")
        })
@Getter
@Setter
public class WorkLog extends BaseEntity {

    /** 由任務帶入，前端送來的值會被忽略。 */
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    /** 建立後不可修改。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    /** 登記者的 user id，由系統填入目前操作者，前端送來的值會被忽略。 */
    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /** 時數，以 0.5 小時為單位；同一人同一天合計不可超過 24。 */
    @NotNull(groups = ValidGroup.Create.class)
    @DecimalMin("0.5")
    @DecimalMax("24")
    @Digits(integer = 2, fraction = 1)
    @Column(name = "hours", nullable = false, precision = 3, scale = 1)
    private BigDecimal hours;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    private String note;
}
