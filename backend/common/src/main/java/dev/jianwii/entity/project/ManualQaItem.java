package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "manual_qa_item")
@Getter
@Setter
public class ManualQaItem extends BaseEntity {
    @Column(name = "batch_id", nullable = false, updatable = false)
    private Long batchId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String steps;

    @Column(name = "expected_result", nullable = false, columnDefinition = "text")
    private String expectedResult;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "assignee_id", length = 100)
    private String assigneeId;
}
