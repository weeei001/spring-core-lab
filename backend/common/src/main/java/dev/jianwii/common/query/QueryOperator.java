package dev.jianwii.common.query;

/**
 * 查詢比較方式，寫死在 Java 端（{@link QueryField} 或 toQueryConditions），API 呼叫端不能自己指定。 轉成 JPA 條件與型別檢查在
 * {@link dev.jianwii.common.dao.QuerySpecifications}。
 *
 * <ul>
 * <li>{@link #EQUAL}／{@link #NOT_EQUAL}：等於／不等於。</li>
 * <li>{@link #LIKE}：包含（{@code LIKE '%x%'}），{@code %}、{@code _} 視為一般字元，不可為空白。</li>
 * <li>{@link #STARTS_WITH}：前綴（{@code LIKE 'x%'}），規則同 LIKE。</li>
 * <li>{@link #GREATER_THAN} 等四種範圍比較：只限數字、日期或時間。</li>
 * <li>{@link #IN}：值為 Collection，任一相同即符合；空集合視為不篩選。</li>
 * </ul>
 */
public enum QueryOperator {
    EQUAL,
    NOT_EQUAL,
    LIKE,
    STARTS_WITH,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    IN
}
