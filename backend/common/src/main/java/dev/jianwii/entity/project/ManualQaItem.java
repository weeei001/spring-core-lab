package dev.jianwii.entity.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** 驗收批次中的檢查項目；有執行紀錄後只能改負責人。 */
@Entity
@Table(
        name = "manual_qa_item",
        indexes = {
            @Index(name = "idx_qa_item_batch_id", columnList = "batch_id,id"),
            @Index(name = "idx_qa_item_task", columnList = "task_id")
        })
@Getter
@Setter
public class ManualQaItem extends BaseEntity {
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "batch_id", nullable = false, updatable = false)
    private Long batchId;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 200)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 10000)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(nullable = false, columnDefinition = "text")
    private String steps;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 10000)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "expected_result", nullable = false, columnDefinition = "text")
    private String expectedResult;

    /** 關聯的同專案任務。 */
    @Column(name = "task_id")
    private Long taskId;

    /** 負責人，須為專案成員。 */
    @Size(max = 100)
    @Column(name = "assignee_id", length = 100)
    private String assigneeId;

    /** 更新時要清空的欄位，只能是 taskId、assigneeId。不存資料庫，也不會回傳。 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(max = 2)
    private Set<@NotNull String> clearFields;

    /** 最新一筆紀錄的結果；沒有紀錄時為 NOT_TESTED。 */
    @Transient
    private String status;

    @Transient
    private ManualQaExecution latestExecution;
}
