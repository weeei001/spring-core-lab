package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 燃盡圖的每日紀錄：進行中的 Sprint 只要任務有異動，就更新當天這一筆（當下的合計）；
 * 沒有異動的日子不寫，畫圖時沿用前一天的值。只算主任務，取消的不計。
 */
@Entity
@Table(name = "sprint_burndown",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sprint_id", "day"}))
@Getter
@Setter
public class SprintBurndown extends BaseEntity {

    @Column(name = "sprint_id", nullable = false, updatable = false)
    private Long sprintId;

    @Column(name = "day", nullable = false, updatable = false)
    private LocalDate day;

    @Column(name = "task_count", nullable = false)
    private Long taskCount;

    @Column(name = "done_task_count", nullable = false)
    private Long doneTaskCount;

    @Column(name = "total_points", nullable = false)
    private Long totalPoints;

    @Column(name = "done_points", nullable = false)
    private Long donePoints;
}
