package dev.jianwii.common.query;

import java.util.List;

/**
 * 單一查詢條件，跟 JPA 無關，由 {@link QueryConditions} 組合。
 */
public sealed interface QueryCondition {

    /**
     * {@code property operator value}。
     *
     * @param property
     *            Entity property 名稱（不是 DTO 欄位名），可用 {@code .} 指到關聯，例如 {@code category.code}
     */
    record Compare(String property, QueryOperator operator, Object value) implements QueryCondition {
    }

    /** 任一條件符合即可（OR）。 */
    record AnyOf(List<QueryCondition> conditions) implements QueryCondition {
    }
}
