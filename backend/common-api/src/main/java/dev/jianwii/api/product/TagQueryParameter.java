package dev.jianwii.api.product;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.product.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 標籤查詢條件，寫法說明見 {@link QueryParameter}。
 */
@Getter
@Setter
public class TagQueryParameter extends QueryParameter<Tag> {

    /** IN：前端拿商品的 tagIds 換成標籤名稱時使用。 */
    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    /** name 包含關鍵字；不可全為空白。 */
    @QueryField(property = "name", operator = QueryOperator.LIKE)
    @Size(max = 50)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;
}
