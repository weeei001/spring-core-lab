package dev.jianwii.project.dao;

import java.math.BigDecimal;

/** 每個任務的工時合計（WorkLogDAO 統計查詢的結果）。 */
public interface TaskHours {

    Long getTaskId();

    BigDecimal getHours();
}
