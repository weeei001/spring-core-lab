package dev.jianwii.common.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 查詢條件清單，彼此以 AND 連接；OR 用 {@link #anyOf} 群組。 由 {@link QueryParameter#toQueryConditions} 產生，DAO 層再轉成
 * JPA 條件。 value 為 null 或空集合時不加條件，呼叫端不必自己判斷。
 */
public class QueryConditions {

    private final List<QueryCondition> conditions = new ArrayList<>();

    public QueryConditions add(String property, QueryOperator operator, Object value) {
        if (value != null && !(value instanceof Collection<?> values && values.isEmpty())) {
            conditions.add(new QueryCondition.Compare(property, operator, value));
        }
        return this;
    }

    public QueryConditions equal(String property, Object value) {
        return add(property, QueryOperator.EQUAL, value);
    }

    /** group 內任一條件符合即可；group 沒有條件時忽略。 */
    public QueryConditions anyOf(QueryConditions group) {
        if (!group.isEmpty()) {
            conditions.add(new QueryCondition.AnyOf(group.getConditions()));
        }
        return this;
    }

    /**
     * 關鍵字搜尋：任一 properties 包含 keyword 即符合，給前端單一搜尋框使用；keyword 為 null 時不篩選。
     */
    public QueryConditions keyword(String keyword, String... properties) {
        QueryConditions group = new QueryConditions();
        for (String property : properties) {
            group.add(property, QueryOperator.LIKE, keyword);
        }
        return anyOf(group);
    }

    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    public List<QueryCondition> getConditions() {
        return List.copyOf(conditions);
    }
}
