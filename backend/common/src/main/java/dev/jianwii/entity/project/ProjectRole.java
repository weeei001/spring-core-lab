package dev.jianwii.entity.project;

/** 成員在專案內的角色；和功能權限（Permission）是兩回事，只在該專案內有效。 */
public enum ProjectRole {
    /** 經理：維護專案資料、成員、里程碑 */
    MANAGER,
    /** 成員：維護任務、留言、登記工時 */
    MEMBER
}
