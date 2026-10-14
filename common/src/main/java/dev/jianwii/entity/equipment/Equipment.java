package dev.jianwii.entity.equipment;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "equipment")
@Getter
@Setter
public class Equipment extends BaseEntity {

    // 建立時必填；更新時可省略（null 表示不改），但不可為空白字串
    /** 設備代碼，不可重複。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
