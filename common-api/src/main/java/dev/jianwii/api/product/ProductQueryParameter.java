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
 * 商品查詢，各欄位示範一種條件寫法；未給的欄位不篩選，條件之間為 AND。
 */
@Getter
@Setter
public class ProductQueryParameter extends QueryParameter<Product> {

    /** 同名 EQUAL：不加註解。 */
    @Size(max = 50)
    private String code;

    @Size(max = 50)
    private String sku;

    /** OR：code、sku 或 name 包含關鍵字，在 {@link #toQueryConditions} 自行組合。 */
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

    @AssertTrue(message = "minPrice 不可大於 maxPrice")
    private boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    @AssertTrue(message = "fromTime 必須小於 toTime")
    private boolean isTimeRangeValid() {
        return fromTime == null || toTime == null || fromTime.isBefore(toTime);
    }
}
