package dev.jianwii.entity.project;

/** Sprint 狀態：規劃中 → 進行中 → 已結束；同一專案同時只能有一個進行中。 */
public enum SprintStatus {
    PLANNED, ACTIVE, CLOSED
}
