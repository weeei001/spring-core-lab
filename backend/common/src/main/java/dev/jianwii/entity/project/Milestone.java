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

/** 里程碑：專案的階段節點，任務可以掛在里程碑下（{@link Task#getMilestoneId()}）。 */
@Entity
@Table(name = "milestone", indexes = @Index(columnList = "project_id"))
@Getter
@Setter
public class Milestone extends BaseEntity {

    /** 所屬專案；建立後不可修改。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** 是否已達成；新增時不給為 false。 */
    @Column(name = "achieved", nullable = false)
    private Boolean achieved;

    // ---- 以下為查詢時計算，不存資料庫；只算主任務，取消的任務不計 ----

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long taskCount;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long doneTaskCount;
}
