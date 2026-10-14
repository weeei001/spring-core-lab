package dev.jianwii.common.dao;

import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.query.QueryCondition;
import dev.jianwii.common.query.QueryConditions;
import dev.jianwii.common.query.QueryOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ClassUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 把 {@link QueryConditions} 轉成 JPA {@link Specification}，並檢查條件值與 Entity property 型別相符。
 * 型別不符、property 不存在屬於程式設定錯誤（IllegalStateException）；使用者輸入錯誤丟 {@link BusinessException}。
 */
public final class QuerySpecifications {

    private QuerySpecifications() {
    }

    public static <T> Specification<T> of(QueryConditions conditions) {
        List<QueryCondition> list = conditions.getConditions();
        return (root, query, builder) -> builder.and(
                list.stream().map(condition -> toPredicate(root, builder, condition))
                        .toArray(Predicate[]::new));
    }

    private static Predicate toPredicate(Root<?> root, CriteriaBuilder builder,
            QueryCondition condition) {
        return switch (condition) {
            case QueryCondition.Compare compare -> compare(root, builder, compare);
            case QueryCondition.AnyOf anyOf -> builder.or(anyOf.conditions().stream()
                    .map(child -> toPredicate(root, builder, child)).toArray(Predicate[]::new));
        };
    }

    private static Predicate compare(Root<?> root, CriteriaBuilder builder,
            QueryCondition.Compare condition) {
        String property = condition.property();
        Path<?> path = path(root, property);
        Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(path.getJavaType());
        if (!supports(type)) {
            throw new IllegalStateException("不支援查詢此欄位型別：" + property);
        }
        QueryOperator operator = condition.operator();
        Object value = condition.value();
        return switch (operator) {
            case EQUAL -> builder.equal(path, typed(property, type, value));
            case NOT_EQUAL -> builder.notEqual(path, typed(property, type, value));
            case LIKE, STARTS_WITH -> like(builder, path, property, typed(property, type, value),
                    operator == QueryOperator.LIKE);
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL -> {
                if (!Number.class.isAssignableFrom(type) && type != LocalDateTime.class) {
                    throw new IllegalStateException("範圍查詢條件必須對應數字或時間：" + property);
                }
                yield range(builder, path, operator, typed(property, type, value));
            }
            case IN -> {
                if (!(value instanceof Collection<?> values)) {
                    throw new IllegalStateException("IN 條件的值必須是 Collection：" + property);
                }
                yield path.in(values.stream().map(item -> typed(property, type, item)).toList());
            }
        };
    }

    private static Predicate like(CriteriaBuilder builder, Path<?> path, String property,
            Object value, boolean contains) {
        if (!(value instanceof String text)) {
            throw new IllegalStateException("文字查詢條件必須對應 String：" + property);
        }
        if (text.codePoints().allMatch(
                character -> Character.isWhitespace(character) || Character.isSpaceChar(character))) {
            throw new BusinessException("查詢文字不可空白");
        }
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return builder.like(path.as(String.class), (contains ? "%" : "") + escaped + "%", '\\');
    }

    @SuppressWarnings("unchecked")
    private static <V extends Comparable<? super V>> Predicate range(CriteriaBuilder builder,
            Path<?> path, QueryOperator operator, Object value) {
        Expression<V> expression = (Expression<V>) path;
        V typedValue = (V) value;
        return switch (operator) {
            case GREATER_THAN -> builder.greaterThan(expression, typedValue);
            case GREATER_THAN_OR_EQUAL -> builder.greaterThanOrEqualTo(expression, typedValue);
            case LESS_THAN -> builder.lessThan(expression, typedValue);
            case LESS_THAN_OR_EQUAL -> builder.lessThanOrEqualTo(expression, typedValue);
            default -> throw new IllegalStateException("不是範圍比較：" + operator);
        };
    }

    /** 值必須與 property 同型別；字串 id／*Id 對到 Long 時轉成 Long。 */
    private static Object typed(String property, Class<?> type, Object value) {
        String name = property.substring(property.lastIndexOf('.') + 1);
        if (type == Long.class && value instanceof String text && (name.equals("id")
                || name.endsWith("Id"))) {
            return queryId(text);
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException("查詢欄位型別設定錯誤：" + property);
        }
        return value;
    }

    /** 把字串 ID 轉成 Long；格式錯誤或小於 1 時丟 {@link BusinessException}。 */
    private static Long queryId(String value) {
        try {
            Long id = Long.valueOf(value);
            if (id < 1) {
                throw new BusinessException("ID 超出有效範圍");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException("ID 超出有效範圍");
        }
    }

    private static boolean supports(Class<?> type) {
        return type == String.class || type == Boolean.class || Number.class.isAssignableFrom(type)
                || type.isEnum() || type == LocalDateTime.class;
    }

    /** 依 {@code .} 逐層取得 property，例如 {@code category.code}。 */
    private static Path<?> path(Root<?> root, String property) {
        try {
            Path<?> path = root;
            for (String part : property.split("\\.")) {
                path = path.get(part);
            }
            return path;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("無法映射查詢欄位：" + property, exception);
        }
    }
}
