package dev.jianwii.common.query;

import dev.jianwii.common.exception.BusinessException;
import jakarta.persistence.criteria.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 查詢 DTO 基底：分頁參數與欄位條件統一轉為 JPA {@link Specification}。 同名、同型別的非 null 欄位自動比對
 * EQUAL，false 與 0 仍有效；字串 id／*Id 可轉為 Long。 特殊條件須加 {@link QueryIgnore}，再以
 * {@link TimeRangeQuery}、{@link NameLikeQuery} 或覆寫 {@link #toQueryConditions}
 * 補上。覆寫時須先呼叫 super。 page 從 1 起算；page、size、sort 不參與篩選。
 *
 * @param <T>
 *            要查詢的 Entity
 */
@Getter
@Setter
public abstract class QueryParameter<T> {

    @QueryIgnore
    @Min(1)
    private int page = 1;
    @QueryIgnore
    @Min(1)
    @Max(100)
    private int size = 20;
    @QueryIgnore
    @NotNull
    @Size(max = 100)
    @Valid
    private List<@NotNull Order> sort = new ArrayList<>();

    /** Service 呼叫此方法取得查詢條件；要擴充條件請覆寫 {@link #toQueryConditions}。 */
    public final Specification<T> toSpecification() {
        return (root, query, builder) -> builder.and(
                toQueryConditions(root, query, builder).toArray(new Predicate[0]));
    }

    /**
     * 產生所有 AND 條件：自動 EQUAL 欄位，加上 DTO 實作的 {@link TimeRangeQuery}、
     * {@link NameLikeQuery} 條件。
     *
     * <p>
     * 子類別覆寫時必須先呼叫 super，再把自己的條件加進回傳的 list。
     */
    protected List<Predicate> toQueryConditions(Root<T> root, CriteriaQuery<?> query,
            CriteriaBuilder builder) {
        List<Predicate> conditions = new ArrayList<>();
        for (Class<?> type = getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(
                        field.getModifiers()) || field.isSynthetic()
                        || field.isAnnotationPresent(
                                QueryIgnore.class)) {
                    continue;
                }
                Path<?> path = queryPath(root, field.getName());
                Class<?> propertyType = path.getJavaType();
                boolean idConversion =
                        field.getType() == String.class && propertyType == Long.class && (field.getName()
                                .equals("id") || field.getName().endsWith("Id"));
                if (!QueryOperator.supports(propertyType) || field.getType()
                        .isPrimitive() || (!idConversion && field.getType() != propertyType)) {
                    throw new IllegalStateException("查詢欄位型別設定錯誤：" + field.getName());
                }
                if (!field.trySetAccessible()) {
                    throw new IllegalStateException("無法讀取查詢欄位：" + field.getName());
                }
                Object value = ReflectionUtils.getField(field, this);
                if (value != null) {
                    conditions.add(QueryOperator.EQUAL.toPredicate(path,
                            idConversion ? queryId((String) value) : value, builder));
                }
            }
        }
        if (this instanceof TimeRangeQuery timeRange) {
            addTimeRange(conditions, root, builder, timeRange);
        }
        if (this instanceof NameLikeQuery nameLike) {
            addCondition(conditions, root, builder, nameLike.nameProperty(),
                    nameLike.likeOperator(), fieldValue(nameLike.nameField(), String.class));
        }
        return conditions;
    }

    /**
     * {@code value} 不為 null 時，對 Entity 的 {@code property} 加一個 {@code operator} 條件。
     *
     * @param property
     *            Entity property 名稱，不是 DTO 欄位名
     * @param value
     *            DTO 欄位值；null 表示不篩選
     */
    protected void addCondition(List<Predicate> conditions, Root<T> root, CriteriaBuilder builder,
            String property, QueryOperator operator, Object value) {
        if (value != null) {
            conditions.add(operator.toPredicate(queryPath(root, property), value, builder));
        }
    }

    /**
     * 關鍵字搜尋：{@code from} 的任一 {@code properties} 包含 {@code keyword} 即符合（OR）。
     * 給前端單一搜尋框使用；跳脫與空白規則同 {@link QueryOperator#LIKE}。
     *
     * @param from
     *            本查詢的 root，或子查詢的 root（例如以姓名找使用者 ID）
     */
    protected static Predicate keywordMatch(From<?, ?> from, CriteriaBuilder builder,
            String keyword, String... properties) {
        Predicate[] matches = new Predicate[properties.length];
        for (int index = 0; index < properties.length; index++) {
            matches[index] = QueryOperator.LIKE.toPredicate(from.get(properties[index]), keyword,
                    builder);
        }
        return builder.or(matches);
    }

    /** 把字串 ID 轉成 Long；格式錯誤或小於 1 時丟 {@link BusinessException}。 */
    protected Long queryId(String value) {
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

    private void addTimeRange(List<Predicate> conditions, Root<T> root, CriteriaBuilder builder,
            TimeRangeQuery timeRange) {
        String fromName = timeRange.fromTimeName();
        String toName = timeRange.toTimeName();
        LocalDateTime from = fieldValue(fromName, LocalDateTime.class);
        LocalDateTime to = fieldValue(toName, LocalDateTime.class);
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BusinessException(
                    fromName + " 必須小於 " + toName + "，使用 [" + fromName + "," + toName + ")");
        }
        addCondition(conditions, root, builder, timeRange.timeProperty(),
                QueryOperator.GREATER_THAN_OR_EQUAL, from);
        addCondition(conditions, root, builder, timeRange.timeProperty(), QueryOperator.LESS_THAN,
                to);
    }

    /**
     * 依名稱讀取本 DTO（含父類別）的欄位值，供 {@link TimeRangeQuery}、{@link NameLikeQuery}
     * 這類「欄位名可覆寫」的 interface 使用。
     *
     * @param name
     *            DTO 欄位名
     * @param type
     *            欄位宣告型別，必須完全相同
     * @throws IllegalStateException
     *             欄位不存在或型別不符；屬於程式設定錯誤，不是使用者輸入錯誤
     */
    private <V> V fieldValue(String name, Class<V> type) {
        for (Class<?> owner = getClass(); owner != Object.class; owner = owner.getSuperclass()) {
            Field field;
            try {
                field = owner.getDeclaredField(name);
            } catch (NoSuchFieldException exception) {
                continue;
            }
            if (field.getType() != type || !field.trySetAccessible()) {
                throw new IllegalStateException("查詢欄位設定錯誤：" + name);
            }
            return type.cast(ReflectionUtils.getField(field, this));
        }
        throw new IllegalStateException("找不到查詢欄位：" + name);
    }

    private Path<?> queryPath(Root<T> root, String property) {
        try {
            return root.get(property);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("無法映射查詢欄位：" + property, exception);
        }
    }

    @Getter
    @Setter
    public static class Order {

        @NotBlank
        private String field;
        @NotNull
        @Pattern(regexp = "asc|desc")
        private String direction = "asc";
    }
}
