package dev.jianwii.common.query;

import dev.jianwii.common.exception.BusinessException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;

/**
 * 查詢比較方式，只供 Java 端組合條件使用，不接受 API 呼叫端傳入 operator。
 *
 * <ul>
 * <li>{@link #EQUAL}：等於。</li>
 * <li>{@link #LIKE}：包含（{@code LIKE '%x%'}），{@code %}、{@code _} 視為一般字元。</li>
 * <li>{@link #STARTS_WITH}：前綴（{@code LIKE 'x%'}），跳脫規則同 LIKE。</li>
 * <li>{@link #GREATER_THAN_OR_EQUAL}／{@link #LESS_THAN}：範圍，只限數字或時間。</li>
 * </ul>
 */
public enum QueryOperator {

    EQUAL, LIKE, STARTS_WITH, GREATER_THAN_OR_EQUAL, LESS_THAN;

    public static boolean supports(Class<?> type) {
        return type == String.class || type == Boolean.class || Number.class.isAssignableFrom(
                type) || type.isEnum() || type == LocalDateTime.class;
    }

    public Predicate toPredicate(Path<?> path, Object value, CriteriaBuilder builder) {
        Class<?> type = path.getJavaType();
        if (!supports(type) || !type.isInstance(value)) {
            throw new IllegalStateException("查詢條件型別設定錯誤");
        }
        return switch (this) {
            case EQUAL -> builder.equal(path, value);
            case LIKE, STARTS_WITH -> {
                if (type != String.class) {
                    throw new IllegalStateException("文字查詢條件必須對應 String");
                }
                String text = (String) value;
                if (text.codePoints().allMatch(
                        character -> Character.isWhitespace(character) || Character.isSpaceChar(
                                character))) {
                    throw new BusinessException("查詢文字不可空白");
                }
                String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                // LIKE 為字面包含；STARTS_WITH 維持前綴比對的既有契約。
                yield builder.like(path.as(String.class), (this == LIKE ? "%" : "") + escaped + "%",
                        '\\');
            }
            case GREATER_THAN_OR_EQUAL, LESS_THAN -> {
                if (!Number.class.isAssignableFrom(type) && type != LocalDateTime.class) {
                    throw new IllegalStateException("範圍查詢條件必須對應數字或時間");
                }
                yield compare(path, value, builder);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <V extends Comparable<? super V>> Predicate compare(Path<?> path, Object value,
            CriteriaBuilder builder) {
        Expression<V> expression = (Expression<V>) path;
        V typedValue = (V) value;
        return this == GREATER_THAN_OR_EQUAL ? builder.greaterThanOrEqualTo(expression, typedValue)
                : builder.lessThan(expression, typedValue);
    }
}
