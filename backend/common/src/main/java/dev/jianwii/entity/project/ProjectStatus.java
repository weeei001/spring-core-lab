package dev.jianwii.entity.project;

import java.util.Set;

/** 專案狀態。結案（DONE）與取消（CANCELLED）後專案唯讀，只能重新開啟回到 ACTIVE。 */
public enum ProjectStatus {
    /** 規劃中 */
    PLANNING,
    /** 進行中 */
    ACTIVE,
    /** 暫停 */
    ON_HOLD,
    /** 結案 */
    DONE,
    /** 取消 */
    CANCELLED;

    /** 可以從目前狀態改成哪些狀態。 */
    public Set<ProjectStatus> next() {
        return switch (this) {
            case PLANNING -> Set.of(ACTIVE, ON_HOLD, CANCELLED);
            case ACTIVE -> Set.of(DONE, ON_HOLD, CANCELLED);
            case ON_HOLD -> Set.of(ACTIVE, CANCELLED);
            // 重新開啟
            case DONE, CANCELLED -> Set.of(ACTIVE);
        };
    }

    /** 結案或取消：專案底下的資料都不能再修改。 */
    public boolean isClosed() {
        return this == DONE || this == CANCELLED;
    }
}
