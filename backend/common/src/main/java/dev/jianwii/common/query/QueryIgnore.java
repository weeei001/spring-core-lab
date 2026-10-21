package dev.jianwii.common.query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 讓 {@link QueryParameter} 不把此欄位轉成查詢條件。
 *
 * <p>
 * 欄位仍會接收 JSON、仍會執行 Bean Validation。用在 page、size 這類非篩選參數， 或要覆寫
 * {@link QueryParameter#toQueryConditions} 自行組條件的欄位（例如 OR、關鍵字搜尋）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface QueryIgnore {}
