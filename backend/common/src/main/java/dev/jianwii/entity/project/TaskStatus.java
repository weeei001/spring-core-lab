package dev.jianwii.entity.project;

/** 任務狀態，也是看板的欄位；可任意切換，沒有固定流程。 */
public enum TaskStatus {
    /** 待辦 */
    TODO,
    /** 進行中 */
    IN_PROGRESS,
    /** 審核中（code review、驗收） */
    REVIEW,
    /** 完成 */
    DONE,
    /** 取消；不算進度 */
    CANCELLED;

    /** 完成或取消：不再算逾期，移除成員時也不會被改成未指派。 */
    public boolean isFinished() {
        return this == DONE || this == CANCELLED;
    }
}
