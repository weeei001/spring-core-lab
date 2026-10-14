package dev.jianwii.api.basicdata;

import dev.jianwii.common.query.QueryConditions;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.basicdata.SerialNumber;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 編號規則查詢條件：serialKey 完全相符；keyword 為 serialKey 或 name 任一包含即符合。
 * 未給的欄位不篩選，寫法說明見 {@link QueryParameter}。
 */
@Getter
@Setter
public class SerialNumberQueryParameter extends QueryParameter<SerialNumber> {

    @Size(max = 50)
    private String serialKey;

    /** 不自動轉成條件（@QueryIgnore），在 {@link #toQueryConditions} 組成 OR；不可全為空白。 */
    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;

    @Override
    public QueryConditions toQueryConditions() {
        return super.toQueryConditions().keyword(keyword, "serialKey", "name");
    }
}
