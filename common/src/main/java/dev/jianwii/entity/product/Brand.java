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

import java.io.Serializable;

/**
 * 品牌，多對一以 code 關聯的範例：{@link Product#getBrandCode()} 存的是這裡的 code，不是 id。
 * code 由使用者自訂、可讀且不隨資料庫變動，適合匯入資料或跨系統對照。
 * 實作 Serializable：Hibernate 以非主鍵欄位（code）關聯 Entity 時需要。
 */
@Entity
@Table(name = "brand")
@Getter
@Setter
public class Brand extends BaseEntity implements Serializable {

    /**
     * 品牌代碼，例如 APPLE；商品以此關聯，所以建立後不可修改。
     * 刪除後也不能再用（唯一約束含已刪除），否則新品牌會接收到舊品牌的商品。
     */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "[A-Z0-9_]+")
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    /** 品牌名稱，不可全為空白。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
