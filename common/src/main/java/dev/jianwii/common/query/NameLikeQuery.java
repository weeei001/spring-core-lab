package dev.jianwii.common.query;

/**
 * 名稱前綴查詢能力：DTO 宣告 {@code @QueryIgnore String namePrefix}，預設比對 Entity 的 name。
 * 可覆寫欄位名、屬性名與文字運算子；欄位仍須是 String。跳脫規則見 {@link QueryOperator}。
 */
public interface NameLikeQuery {

    /** DTO 欄位名（即 JSON 參數名），預設 {@code namePrefix}。 */
    default String nameField() {
        return "namePrefix";
    }

    /** 要比對的 Entity property 名稱，預設 {@code name}。 */
    default String nameProperty() {
        return "name";
    }

    /**
     * 比對方式，預設 {@link QueryOperator#STARTS_WITH}（{@code name LIKE 'x%'}）； 改成
     * {@link QueryOperator#LIKE} 則為包含（{@code name LIKE '%x%'}）。
     */
    default QueryOperator likeOperator() {
        return QueryOperator.STARTS_WITH;
    }
}
