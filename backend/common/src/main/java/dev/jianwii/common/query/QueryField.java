package dev.jianwii.common.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定 DTO 欄位對應的 Entity property 與比較方式；沒加時為同名 EQUAL。
 *
 * <p>
 * 時間區間 [fromTime, toTime) 宣告兩個欄位，起訖檢查用 {@code @AssertTrue}：
 *
 * <pre>
 * {@code
 * &#64;QueryField(property = "createTime", operator = QueryOperator.GREATER_THAN_OR_EQUAL)
 * private LocalDateTime fromTime;
 *
 * &#64;QueryField(property = "createTime", operator = QueryOperator.LESS_THAN)
 * private LocalDateTime toTime;
 *
 * @AssertTrue(message = "fromTime 必須小於 toTime")
 * private boolean isTimeRangeValid() {
 *     return fromTime == null || toTime == null || fromTime.isBefore(toTime);
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface QueryField {

    /** Entity property 名稱，可用 {@code .} 指到關聯；預設與欄位同名。 */
    String property() default "";

    QueryOperator operator() default QueryOperator.EQUAL;
}
