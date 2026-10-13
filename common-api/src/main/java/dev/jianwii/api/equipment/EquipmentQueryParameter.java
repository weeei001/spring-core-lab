package dev.jianwii.api.equipment;

import dev.jianwii.common.query.NameLikeQuery;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.equipment.Equipment;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * code 完全相同；namePrefix 比對 name 前綴。
 */
@Getter
@Setter
public class EquipmentQueryParameter extends QueryParameter<Equipment> implements NameLikeQuery {

    @Size(max = 50)
    private String code;

    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String namePrefix;
}
