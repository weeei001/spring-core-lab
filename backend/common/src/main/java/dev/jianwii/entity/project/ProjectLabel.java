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

/**
 * 專案標籤，例如 frontend、backend、API；每個專案各自維護，任務可掛多個（{@link Task#getLabelIds()}）。
 * 同一專案內名稱不可重複（不分大小寫）。
 */
@Entity
@Table(name = "project_label", indexes = @Index(columnList = "project_id"))
@Getter
@Setter
public class ProjectLabel extends BaseEntity {

    /** 所屬專案；建立後不可修改。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 30)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 30)
    private String name;

    /** 標籤顏色，例如 #1677FF。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Pattern(regexp = "#[0-9A-Fa-f]{6}")
    @Column(name = "color", nullable = false, length = 7)
    private String color;

    // ---- 以下為查詢時計算，不存資料庫 ----

    /** 掛了這個標籤的任務數（含子任務，取消的不計）。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long taskCount;
}
