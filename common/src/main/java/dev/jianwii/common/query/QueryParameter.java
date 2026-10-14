package dev.jianwii.common.query;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 查詢 DTO 基底：分頁參數，以及把欄位轉成 {@link QueryConditions}（跟 JPA 無關，DAO 層再轉成查詢）。
 *
 * <ul>
 * <li>非 null 欄位自動成為條件，預設同名 EQUAL；false 與 0 仍有效。</li>
 * <li>欄位名或比較方式不同時加 {@link QueryField}；不當條件的欄位加 {@link QueryIgnore}。</li>
 * <li>OR、關鍵字等特殊條件：覆寫 {@link #toQueryConditions}，先呼叫 super 再加。</li>
 * <li>欄位不可為基本型別（無法表示「不篩選」）；字串 id／*Id 對到 Long 時會自動轉換。</li>
 * </ul>
 * page 從 1 起算；page、size、sort 不參與篩選。
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

    public QueryConditions toQueryConditions() {
        QueryConditions conditions = new QueryConditions();
        for (Class<?> type = getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(
                        field.getModifiers()) || field.isSynthetic() || field.isAnnotationPresent(
                                QueryIgnore.class)) {
                    continue;
                }
                if (field.getType().isPrimitive()) {
                    throw new IllegalStateException("查詢欄位不可為基本型別：" + field.getName());
                }
                if (!field.trySetAccessible()) {
                    throw new IllegalStateException("無法讀取查詢欄位：" + field.getName());
                }
                QueryField queryField = field.getAnnotation(QueryField.class);
                String property = queryField == null || queryField.property().isEmpty()
                        ? field.getName() : queryField.property();
                QueryOperator operator =
                        queryField == null ? QueryOperator.EQUAL : queryField.operator();
                conditions.add(property, operator, ReflectionUtils.getField(field, this));
            }
        }
        return conditions;
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
