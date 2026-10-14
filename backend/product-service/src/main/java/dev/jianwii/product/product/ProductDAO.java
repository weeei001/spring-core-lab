package dev.jianwii.product.product;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ProductDAO extends BaseDAO<Product, Long> {

    /** 含已軟刪除的資料，與資料表的唯一約束一致。 */
    boolean existsBySku(String sku);

    /** 是否有有效商品使用此品牌；刪除品牌前檢查用。 */
    boolean existsByBrandCodeAndDbMark(String brandCode, Integer dbMark);

    /** 是否有有效商品含此標籤；刪除標籤前檢查用。集合欄位用 Containing，會產生 MEMBER OF 條件。 */
    boolean existsByTagIdsContainingAndDbMark(Long tagId, Integer dbMark);

    /** 悲觀鎖（SELECT ... FOR UPDATE），交易結束前其他交易無法修改同一筆；調整庫存用。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockByIdAndDbMark(Long id, Integer dbMark);
}
