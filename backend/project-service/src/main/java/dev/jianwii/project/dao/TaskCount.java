package dev.jianwii.project.dao;

/** 任務分組計數（TaskDAO 統計查詢的結果）；groupId 依查詢而定，是專案、父任務或標籤的 id。 */
public interface TaskCount {

    Long getGroupId();

    Long getTotal();

    Long getDone();

    Long getOverdue();
}
