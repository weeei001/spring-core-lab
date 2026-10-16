package dev.jianwii.entity.notification;

/** 通知的種類，前端依此顯示圖示。 */
public enum NotificationType {
    /** 任務指派給你 */
    TASK_ASSIGNED,
    /** 你負責或關注的任務狀態變更 */
    TASK_STATUS_CHANGED,
    /** 你負責或關注的任務有新留言 */
    TASK_COMMENTED,
    /** 留言中 @ 到你 */
    TASK_MENTIONED,
    /** 管理員發送的公告 */
    ANNOUNCEMENT
}
