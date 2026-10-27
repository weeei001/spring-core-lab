package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "manual_qa_execution")
@Getter
@Setter
public class ManualQaExecution extends BaseEntity {
    @Column(name = "item_id", nullable = false, updatable = false)
    private Long itemId;

    @Column(name = "tester_id", nullable = false, length = 100, updatable = false)
    private String testerId;

    @Column(name = "executed_at", nullable = false, columnDefinition = "datetime(6)", updatable = false)
    private LocalDateTime executedAt;

    @Column(name = "tested_version", nullable = false, length = 200, updatable = false)
    private String testedVersion;

    @Column(nullable = false, length = 16, updatable = false)
    private String result;

    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String actual;

    @Column(name = "bug_task_id")
    private Long bugTaskId;
}
