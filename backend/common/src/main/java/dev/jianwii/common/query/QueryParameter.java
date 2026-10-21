package dev.jianwii.common.query;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.ReflectionUtils;

/**
 * 查詢參數基底（查詢 API 的 request body）：分頁、排序，以及把子類別的欄位自動轉成查詢條件， 子類別只要宣告欄位，不必手寫「有值才加條件」的判斷。
 *
 * <ul>
 * <li>值非 null 的欄位才會變成條件，預設為「Entity 同名欄位 = 值」；false 與 0 也算有值。</li>
 * <li>Entity 欄位名不同或要用其他比較方式時加 {@link QueryField}；不當條件的欄位加 {@link QueryIgnore}。</li>
 * <li>OR、關鍵字等特殊條件：覆寫 {@link #toQueryConditions}，先呼叫 super 再加。</li>
 * <li>欄位不可用基本型別（例如 int 預設為 0，無法表示「不篩選」）；字串的 id／xxxId 對到 Long 時會自動轉換。</li>
 * </ul>
 * page 從 1 起算；page、size、sort 不參與篩選。
 *
 * @param <T> 要查詢的 Entity
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

    /** 用反射讀取所有欄位（含父類別），把有值的欄位轉成查詢條件。 */
    public QueryConditions toQueryConditions() {
        QueryConditions conditions = new QueryConditions();
        for (Class<?> type = getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())
                        || field.isSynthetic()
                        || field.isAnnotationPresent(QueryIgnore.class)) {
                    continue;
                }
                if (field.getType().isPrimitive()) {
                    throw new IllegalStateException("查詢欄位不可為基本型別：" + field.getName());
                }
                if (!field.trySetAccessible()) {
                    throw new IllegalStateException("無法讀取查詢欄位：" + field.getName());
                }
                QueryField queryField = field.getAnnotation(QueryField.class);
                String property =
                        queryField == null || queryField.property().isEmpty() ? field.getName() : queryField.property();
                QueryOperator operator = queryField == null ? QueryOperator.EQUAL : queryField.operator();
                conditions.add(property, operator, ReflectionUtils.getField(field, this));
            }
        }
        return conditions;
    }

    /** 排序：field 為 Entity 欄位名（必須在 Service 的 sortableFields 內），direction 為 asc 或 desc。 */
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
