package dev.jianwii.common.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 讓 {@link QueryParameter} 跳過此欄位的自動 EQUAL 條件。
 *
 * <p>
 * 只影響自動 EQUAL：欄位仍會接收 JSON、仍會執行 Bean Validation。用在：
 * <ul>
 * <li>欄位名和 Entity property 不同，例如 {@code namePrefix}、{@code fromTime}。</li>
 * <li>比對方式不是 EQUAL，例如前綴、範圍、關聯、子查詢。</li>
 * </ul>
 * 加了之後，要由 {@link TimeRangeQuery}、{@link NameLikeQuery}，或覆寫
 * {@link QueryParameter#toQueryConditions} 自行補條件，否則這個參數不會有任何作用。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface QueryIgnore {
}
