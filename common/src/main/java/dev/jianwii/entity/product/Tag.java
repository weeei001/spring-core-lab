package dev.jianwii.entity.product;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 商品標籤，多對多範例：一個商品可有多個標籤，一個標籤也可用在多個商品。
 * 關聯存在 {@link Product#getTagIds()} 的中間表 product_tag，這裡不反向記錄商品。
 */
@Entity
@Table(name = "tag")
@Getter
@Setter
public class Tag extends BaseEntity {

    /** 標籤名稱，例如「新品」；不可全為空白。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 50)
    private String name;
}
