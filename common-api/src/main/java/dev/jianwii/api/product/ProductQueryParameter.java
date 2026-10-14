package dev.jianwii.api.product;

import dev.jianwii.common.query.QueryConditions;
import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.product.Product;
import dev.jianwii.entity.product.ProductCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品查詢條件，各欄位示範一種條件寫法（規則見 {@link QueryParameter}）；未給的欄位不篩選，條件之間為 AND。
 */
@Getter
@Setter
public class ProductQueryParameter extends QueryParameter<Product> {

    /** 沒加 @QueryField：比對 Entity 同名欄位，code = 值。 */
    @Size(max = 50)
    private String code;

    @Size(max = 50)
    private String sku;

    /** OR：code、sku 或 name 任一包含關鍵字；加 @QueryIgnore 不自動轉換，改在 {@link #toQueryConditions} 組合。不可全為空白。 */
    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;

    /** IN：屬於任一分類；空陣列視為不篩選。 */
    @QueryField(property = "category", operator = QueryOperator.IN)
    @Size(max = 20)
    private List<@NotNull ProductCategory> categories;

    /** 範圍：minPrice <= price <= maxPrice。 */
    @QueryField(property = "price", operator = QueryOperator.GREATER_THAN_OR_EQUAL)
    private BigDecimal minPrice;

    @QueryField(property = "price", operator = QueryOperator.LESS_THAN_OR_EQUAL)
    private BigDecimal maxPrice;

    private Boolean onSale;

    /** 時間區間 [fromTime, toTime)。 */
    @QueryField(property = "createTime", operator = QueryOperator.GREATER_THAN_OR_EQUAL)
    private LocalDateTime fromTime;

    @QueryField(property = "createTime", operator = QueryOperator.LESS_THAN)
    private LocalDateTime toTime;

    @Override
    public QueryConditions toQueryConditions() {
        return super.toQueryConditions().keyword(keyword, "code", "sku", "name");
    }

    // @AssertTrue 放在 isXxx 方法上：驗證時會呼叫它，回傳 false 即驗證失敗（用來檢查多個欄位的關係）
    @AssertTrue(message = "minPrice 不可大於 maxPrice")
    private boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    @AssertTrue(message = "fromTime 必須小於 toTime")
    private boolean isTimeRangeValid() {
        return fromTime == null || toTime == null || fromTime.isBefore(toTime);
    }
}
