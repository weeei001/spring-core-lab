package dev.jianwii.entity.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.Map;

/**
 * 專案。只有 project-service 會建這張表（它的 @EntityScan 只掃這個 package），其他服務透過 ProjectApi 取得。
 * 建立者自動成為專案經理（{@link ProjectMember}）。
 *
 * <p>
 * 欄位驗證：{@code groups = ValidGroup.Create.class} 的規則只在新增時檢查（必填）；
 * 沒寫 groups 的規則新增、更新都檢查，但值為 null 時略過，所以更新時不給就是不改。詳見 ValidGroup。
 */
@Entity
@Table(name = "project")
@Getter
@Setter
public class Project extends BaseEntity {

    /** 系統編號，新增時向 basic-data 依 PROJECT 規則取號，前端送來的值會被忽略；不可修改。 */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    /** 專案名稱，不可全為空白。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    /** 新增時不給為 PLANNING；可改成哪些狀態見 {@link ProjectStatus#next()}。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** 看板與甘特圖用來區分專案的顏色，例如 #1677FF。 */
    @Pattern(regexp = "#[0-9A-Fa-f]{6}")
    @Column(name = "color", length = 7)
    private String color;

    /**
     * 看板 WIP 上限：狀態 → 該欄最多幾張主任務，存在中間表 project_wip_limit；只能設 IN_PROGRESS、REVIEW，1–99。
     * 超過時看板該欄亮紅燈，不擋操作。更新時不給（null）表示不改，給 {} 表示全部取消。
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_wip_limit", joinColumns = @JoinColumn(name = "project_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "status", length = 20)
    @Column(name = "max_tasks", nullable = false)
    @BatchSize(size = 100)
    private Map<TaskStatus, Integer> wipLimits;

    // ---- 以下為查詢時計算，不存資料庫；只算主任務（不含子任務），取消的任務不計 ----

    /** 任務總數。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long taskCount;

    /** 已完成的任務數；進度 = doneTaskCount / taskCount。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long doneTaskCount;

    /** 逾期（過了到期日仍未完成）的任務數。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long overdueTaskCount;
}
