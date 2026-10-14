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
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 商品。Entity 放在 common 讓各服務共用類別，但只有 product-service 會建這張表（它的 @EntityScan 只掃這個 package）；
 * 其他服務透過 ProductApi 取得。
 *
 * <p>
 * 欄位驗證：{@code groups = ValidGroup.Create.class} 的規則只在新增時檢查（必填）；
 * 沒寫 groups 的規則新增、更新都檢查，但值為 null 時略過，所以更新時不給就是不改。詳見 ValidGroup。
 *
 * <p>
 * 關聯：前端新增、修改時只送 brandCode、tagIds；品牌另以 {@link #brand} 帶回完整資料。
 * 關聯一律 LAZY（用到才查），回傳前由 ProductService 在交易內載入（見 BaseService.initialize）。
 * 品牌、標籤是否存在由 ProductService 在新增、修改時檢查。
 */
@Entity
@Table(name = "product")
@Getter
@Setter
public class Product extends BaseEntity {

    /** 系統編號，新增時向 basic-data 依 PRODUCT 規則取號，前端送來的值會被忽略；不可修改。 */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = 50)
    private String code;

    /** 商品貨號（使用者自訂），不可重複。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9-]+")
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    /** 商品名稱，不可全為空白。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(groups = ValidGroup.Create.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ProductCategory category;

    /** 售價，最多 8 位整數、2 位小數。 */
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

    /**
     * 多對一：品牌代碼，對應 {@link Brand#getCode()}（用 code 而非 id 關聯），新增、修改品牌時送這個欄位。
     * 選填；設定後無法透過 update 清空（null 表示不改）。
     */
    @Size(max = 50)
    @Column(name = "brand_code", length = 50)
    private String brandCode;

    /**
     * 多對一：品牌資料，和 brandCode 共用 brand_code 欄位，用來把品牌帶回前端。
     * insertable、updatable = false：存檔以 brandCode 為準，前端送來的 brand 由 ProductService 忽略。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_code", referencedColumnName = "code", insertable = false,
            updatable = false)
    private Brand brand;

    /**
     * 多對多：標籤 id，存在中間表 product_tag（product_id, tag_id），對應 {@link Tag#getId()}。
     * 更新時不給（null）表示不改，給 [] 表示清空，給清單則整批取代。
     * {@code @BatchSize}：載入某筆商品的標籤時，順便載入同一批其他商品的標籤，
     * 列表只多一條 SQL，不會每筆商品各多一條（N+1）。
     */
    @Size(max = 20)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_tag", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag_id", nullable = false)
    @BatchSize(size = 100)
    private Set<@NotNull Long> tagIds;
}
