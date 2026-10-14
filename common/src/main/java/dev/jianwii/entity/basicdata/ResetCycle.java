package dev.jianwii.entity.basicdata;

import java.time.LocalDate;
import java.time.YearMonth;

/** 流水號歸零週期；進入新週期後第一次取號從 1 開始。 */
public enum ResetCycle {
    /** 不歸零 */
    NONE,
    /** 每年 */
    YEAR,
    /** 每月 */
    MONTH,
    /** 每日 */
    DAY;

    /** last 與 today 是否在同一週期；NONE 永遠視為同一週期。 */
    public boolean samePeriod(LocalDate last, LocalDate today) {
        return switch (this) {
            case NONE -> true;
            case YEAR -> last.getYear() == today.getYear();
            case MONTH -> YearMonth.from(last).equals(YearMonth.from(today));
            case DAY -> last.equals(today);
        };
    }
}
