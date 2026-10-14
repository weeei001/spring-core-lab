package dev.jianwii.product.brand;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.product.Brand;

import java.util.Optional;

public interface BrandDAO extends BaseDAO<Brand, Long> {

    /** 含已軟刪除的資料，與資料表的唯一約束一致。 */
    boolean existsByCode(String code);

    /** 有效品牌；商品設定 brandCode 時檢查用。 */
    Optional<Brand> findByCodeAndDbMark(String code, Integer dbMark);
}
