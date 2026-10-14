package dev.jianwii.entity.product;

/** 商品分類，資料庫存名稱（{@code @Enumerated(STRING)}），新增值不影響既有資料。 */
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
