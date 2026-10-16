package dev.jianwii.entity.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Sprint（迭代）：一段固定期間要完成的任務，任務以 {@link Task#getSprintId()} 排入；
 * 沒有排入 Sprint 的未完成主任務即為 Backlog。狀態只能用 SprintApi.start／close 改變。
 */
@Entity
@Table(name = "sprint", indexes = @Index(columnList = "project_id, status"))
@Getter
@Setter
public class Sprint extends BaseEntity {

    /** 所屬專案；建立後不可修改。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Sprint 目標。 */
    @Size(max = 1000)
    @Column(name = "goal", length = 1000)
    private String goal;

    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 新增時固定為 PLANNED，前端送來的值會被忽略。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SprintStatus status;

    // ---- 以下為查詢時計算，不存資料庫；只算主任務，取消的不計 ----

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long taskCount;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long doneTaskCount;

    /** 故事點數合計。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long totalPoints;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long donePoints;
}
