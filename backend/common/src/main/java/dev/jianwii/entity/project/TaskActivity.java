package dev.jianwii.entity.project;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 任務活動紀錄，由 project-service 在任務異動時自動寫入，只能查詢；操作者即 createId、時間即 createTime。 值一律存成文字：enum 存名稱、日期存
 * yyyy-MM-dd、人員存 user id，前端再轉成顯示文字。
 */
@Entity
@Table(name = "task_activity", indexes = @Index(columnList = "task_id"))
@Getter
@Setter
public class TaskActivity extends BaseEntity {

    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private TaskActivityType type;

    /** CHANGED 時改了哪個欄位，為 Task 的 property 名稱，例如 status。 */
    @Column(name = "field", updatable = false, length = 50)
    private String field;

    @Column(name = "old_value", updatable = false, length = 500)
    private String oldValue;

    @Column(name = "new_value", updatable = false, length = 500)
    private String newValue;
}
