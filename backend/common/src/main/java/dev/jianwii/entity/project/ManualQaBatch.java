package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 手動驗收批次；統計欄位依各項目最新一筆紀錄計算，不存資料庫。 */
@Entity
@Table(name = "manual_qa_batch", indexes = @Index(name = "idx_qa_batch_project_id", columnList = "project_id,id"))
@Getter
@Setter
public class ManualQaBatch extends BaseEntity {
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 200)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "version_label", nullable = false, length = 200)
    private String versionLabel;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 1000)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(nullable = false, length = 1000)
    private String environment;

    @Transient
    private Long total;

    @Transient
    private Long notTested;

    @Transient
    private Long passed;

    @Transient
    private Long failed;

    @Transient
    private Long blocked;

    /** 通過數／總項目數；沒有項目時為 null。 */
    @Transient
    private Double passRate;
}
