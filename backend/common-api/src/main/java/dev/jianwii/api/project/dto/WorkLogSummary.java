package dev.jianwii.api.project.dto;

import dev.jianwii.api.project.api.WorkLogApi;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@link WorkLogApi#summary} 的結果：某人某天的工時合計。 統計結果沒有對應的 Entity，所以這是少數另建的回傳型別。
 */
public record WorkLogSummary(String userId, LocalDate workDate, BigDecimal hours) {}
