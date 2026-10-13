package dev.jianwii.common.query;

/**
 * 單一時間欄位的半開區間查詢：[fromTime, toTime)，null 代表該側無限制。 DTO 須自行宣告兩個
 * {@code @QueryIgnore LocalDateTime} 欄位，預設比對 createTime；異名可覆寫下列方法。 兩個 Entity
 * 時間欄位的重疊查詢，應改在 {@link QueryParameter#toQueryConditions} 組合條件。
 */
public interface TimeRangeQuery {

    /** 起始時間的 DTO 欄位名（即 JSON 參數名），預設 {@code fromTime}。 */
    default String fromTimeName() {
        return "fromTime";
    }

    /** 結束時間（不含）的 DTO 欄位名（即 JSON 參數名），預設 {@code toTime}。 */
    default String toTimeName() {
        return "toTime";
    }

    /** 要比對的 Entity property 名稱，預設 {@code createTime}。 */
    default String timeProperty() {
        return "createTime";
    }
}
