package dev.jianwii.entity.project;

/** 任務活動的種類。 */
public enum TaskActivityType {
    /** 建立任務 */
    CREATED,
    /** 欄位變更，field、oldValue、newValue 記錄改了什麼 */
    CHANGED,
    /** 新增留言 */
    COMMENTED
}
