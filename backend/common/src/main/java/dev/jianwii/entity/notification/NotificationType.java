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
    /** 請假申請送交審核 */
    LEAVE_SUBMITTED,
    /** 請假核准或退回 */
    LEAVE_REVIEWED,
    /** 管理員發送的公告 */
    ANNOUNCEMENT,
    /** 申請人撤回或銷假 */
    LEAVE_CANCELLED,
    /** 每日逾期及即將到期任務摘要 */
    TASK_DAILY_DIGEST
}
