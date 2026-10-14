package dev.jianwii.api.product;

import dev.jianwii.common.query.QueryConditions;
import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.product.Brand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 品牌查詢條件，寫法說明見 {@link QueryParameter}。
 */
@Getter
@Setter
public class BrandQueryParameter extends QueryParameter<Brand> {

    /** IN：前端拿商品的 brandCode 換成品牌名稱時使用。 */
    @QueryField(property = "code", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull String> codes;

    /** code 或 name 任一包含關鍵字；不可全為空白。 */
    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;

    @Override
    public QueryConditions toQueryConditions() {
        return super.toQueryConditions().keyword(keyword, "code", "name");
    }
}
