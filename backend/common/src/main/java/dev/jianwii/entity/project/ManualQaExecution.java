package dev.jianwii.entity.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** 一次驗收結果；建立後只能修改 BUG 連結。執行人與時間由系統填入。 */
@Entity
@Table(
        name = "manual_qa_execution",
        indexes = {
            @Index(name = "idx_qa_execution_item_time_id", columnList = "item_id,executed_at,id"),
            @Index(name = "idx_qa_execution_bug", columnList = "bug_task_id")
        })
@Getter
@Setter
public class ManualQaExecution extends BaseEntity {
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "item_id", nullable = false, updatable = false)
    private Long itemId;

    @Column(name = "tester_id", nullable = false, length = 100, updatable = false)
    private String testerId;

    @Column(name = "executed_at", nullable = false, columnDefinition = "datetime(6)", updatable = false)
    private LocalDateTime executedAt;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 200)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "tested_version", nullable = false, length = 200, updatable = false)
    private String testedVersion;

    @NotNull(groups = ValidGroup.Create.class)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private ManualQaResult result;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 10000)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String actual;

    /** 只有 FAIL 可連結同專案的 BUG 任務。 */
    @Column(name = "bug_task_id")
    private Long bugTaskId;

    /** 更新時要清空的欄位，只能是 bugTaskId。不存資料庫，也不會回傳。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(max = 1)
    private Set<@NotNull String> clearFields;
}
