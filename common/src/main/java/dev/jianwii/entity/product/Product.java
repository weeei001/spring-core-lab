package dev.jianwii.entity.product;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 商品。資料表只在 product-service（@EntityScan 此 package）；其他服務透過 ProductApi 取得。
 */
@Entity
@Table(name = "product")
@Getter
@Setter
public class Product extends BaseEntity {

    /** 系統編號，新增時向 basic-data 依 PRODUCT 規則取號，前端送來的值會被忽略；不可修改。 */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    // 建立時必填；更新時可省略（null 表示不改），但不可為空白字串
    /** 商品貨號（使用者自訂），不可重複。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9-]+")
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(groups = ValidGroup.Create.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ProductCategory category;

    @NotNull(groups = ValidGroup.Create.class)
    @DecimalMin("0")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** 建立時給初始庫存；之後只能透過 ProductApi.adjustStock 增減，update 不接受。 */
    @NotNull(groups = ValidGroup.Create.class)
    @PositiveOrZero
    @Column(name = "stock", nullable = false)
    private Integer stock;

    /** 是否上架。 */
    @NotNull(groups = ValidGroup.Create.class)
    @Column(name = "on_sale", nullable = false)
    private Boolean onSale;
}
