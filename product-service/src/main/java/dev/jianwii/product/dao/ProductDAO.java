package dev.jianwii.product.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ProductDAO extends BaseDAO<Product, Long> {

    boolean existsBySku(String sku);

    /** SELECT ... FOR UPDATE，交易結束前其他人無法修改同一筆；調整庫存用。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockById(Long id);
}
