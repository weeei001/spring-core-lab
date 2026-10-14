package dev.jianwii.entity.product;

/** 商品分類。資料庫存的是名稱字串（{@code @Enumerated(STRING)}）而非序號，新增或調整順序都不影響既有資料。 */
public enum ProductCategory {
    /** 3C 電子 */
    ELECTRONICS,
    /** 書籍 */
    BOOK,
    /** 食品 */
    FOOD,
    /** 服飾 */
    CLOTHING,
    /** 其他 */
    OTHER
}
