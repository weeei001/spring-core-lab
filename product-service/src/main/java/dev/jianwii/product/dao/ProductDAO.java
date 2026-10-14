package dev.jianwii.product.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductDAO extends BaseDAO<Product, Long> {

    /** 含已軟刪除的資料，與資料表的唯一約束一致。 */
    boolean existsBySku(String sku);

    List<Product> findAllByIdInAndDbMark(Collection<Long> ids, Integer dbMark);

    /** SELECT ... FOR UPDATE，交易結束前其他人無法修改同一筆；調整庫存用。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockByIdAndDbMark(Long id, Integer dbMark);
}
