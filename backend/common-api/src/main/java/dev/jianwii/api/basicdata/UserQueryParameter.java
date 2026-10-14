package dev.jianwii.api.basicdata;

import dev.jianwii.common.query.QueryConditions;
import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.basicdata.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 使用者查詢條件，寫法說明見 {@link QueryParameter}。
 */
@Getter
@Setter
public class UserQueryParameter extends QueryParameter<User> {

    /**
     * IN：前端拿 createId、transId 換成名字時使用；字串會自動轉成 Long。
     * 系統操作記為 system，不是使用者，送出前請先排除，否則回 400。
     */
    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull String> ids;

    /** username、name 或 email 任一包含關鍵字；不可全為空白。 */
    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;

    @Override
    public QueryConditions toQueryConditions() {
        return super.toQueryConditions().keyword(keyword, "username", "name", "email");
    }
}
