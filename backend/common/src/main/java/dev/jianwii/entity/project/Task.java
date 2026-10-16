package dev.jianwii.entity.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 任務。parentId 有值的是子任務；子任務只有一層，子任務底下不能再有子任務。
 * 類型、標籤、狀態、指派人、優先度、日期、里程碑的變更由 TaskService 自動寫進活動紀錄（{@link TaskActivity}）。
 * 狀態變更與新留言會通知指派人與關注者（{@link #watcherIds}）。
 *
 * <p>
 * 更新是部分更新（null 表示不改），要清空選填欄位時把欄位名稱放進 {@link #clearFields}。
 */
@Entity
@Table(name = "task", indexes = {@Index(columnList = "project_id, status"),
        @Index(columnList = "assignee_id"), @Index(columnList = "parent_id")})
@Getter
@Setter
public class Task extends BaseEntity {

    /** 系統編號，新增時向 basic-data 依 TASK 規則取號，前端送來的值會被忽略；不可修改。 */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    /** 所屬專案；建立後不可修改。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    /** 所屬里程碑，必須是同一專案的里程碑；選填。 */
    @Column(name = "milestone_id")
    private Long milestoneId;

    /** 父任務；有值即為子任務。建立後不可修改。 */
    @Column(name = "parent_id", updatable = false)
    private Long parentId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 200)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 5000)
    @Column(name = "description", length = 5000)
    private String description;

    /** 新增時不給為 FEATURE；欄位預設值讓既有資料在加欄位時補成 FEATURE。 */
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'FEATURE'")
    @Column(name = "type", nullable = false, length = 20)
    private TaskType type;

    /** 新增時不給為 TODO；看板拖曳請用 TaskApi.move，才會一併調整排序。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    /** 新增時不給為 MEDIUM。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    /** 指派人的 user id，必須是該專案的成員；選填。 */
    @Size(max = 100)
    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    /** 到期日；過了到期日仍未完成即為逾期。 */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** 預估工時（小時），最多 1 位小數。 */
    @DecimalMin("0")
    @Digits(integer = 5, fraction = 1)
    @Column(name = "estimate_hours", precision = 6, scale = 1)
    private BigDecimal estimateHours;

    /** 看板同一欄內的順序，小的在上；由系統維護，前端送來的值會被忽略。 */
    @Column(name = "sort_order", nullable = false)
    private Long sortOrder;

    /**
     * 前置任務 id（finish-to-start：前置任務完成後才開始），存在中間表 task_predecessor；
     * 必須是同一專案的任務，且不能形成循環。更新時不給（null）表示不改，給 [] 表示清空。
     */
    @Size(max = 50)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_predecessor", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "predecessor_id", nullable = false)
    @BatchSize(size = 100)
    private Set<@NotNull Long> predecessorIds;

    /**
     * 標籤 id（{@link ProjectLabel}），存在中間表 task_label；必須是同一專案的標籤。
     * 更新時不給（null）表示不改，給 [] 表示清空。
     */
    @Size(max = 20)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_label", joinColumns = @JoinColumn(name = "task_id"),
            indexes = @Index(columnList = "label_id"))
    @Column(name = "label_id", nullable = false)
    @BatchSize(size = 100)
    private Set<@NotNull Long> labelIds;

    /**
     * 關注者的 user id，存在中間表 task_watcher；由系統維護，新增、修改時前端送來的值會被忽略。
     * 建立者、指派人、留言者、被 @ 的人會自動加入；自己關注、取消關注用 TaskApi.watch／unwatch。
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_watcher", joinColumns = @JoinColumn(name = "task_id"),
            indexes = @Index(columnList = "user_id"))
    @Column(name = "user_id", nullable = false, length = 100)
    @BatchSize(size = 100)
    private Set<String> watcherIds;

    /**
     * 只在更新時使用：要清空的選填欄位，可用 milestoneId、assigneeId、startDate、dueDate、estimateHours、description。
     * 不存資料庫，也不會回傳。
     */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(max = 10)
    private Set<@NotNull String> clearFields;

    // ---- 以下為查詢時計算，不存資料庫 ----

    /** 子任務數（取消的不計）；子任務本身沒有。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long subtaskCount;

    /** 已完成的子任務數。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long doneSubtaskCount;

    /** 已登記的工時合計（小時）。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal loggedHours;
}
