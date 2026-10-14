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
 * serialKey 完全相同；keyword 比對 serialKey 或 name。
 */
@Getter
@Setter
public class SerialNumberQueryParameter extends QueryParameter<SerialNumber> {

    @Size(max = 50)
    private String serialKey;

    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;

    @Override
    public QueryConditions toQueryConditions() {
        return super.toQueryConditions().keyword(keyword, "serialKey", "name");
    }
}
