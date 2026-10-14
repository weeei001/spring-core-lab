package dev.jianwii.product.brand;

import dev.jianwii.api.product.BrandQueryParameter;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.product.Brand;
import dev.jianwii.entity.product.Product;
import dev.jianwii.product.product.ProductDAO;
import org.springframework.stereotype.Service;

/** 品牌 CRUD。商品以 code 關聯品牌，所以 code 不可修改、不可重複使用，有商品使用時不可刪除。 */
@Service
public class BrandService extends BaseService<Brand, BrandQueryParameter> {
    private final BrandDAO brandDAO;
    // 用 DAO 而非 ProductService，避免兩個 Service 互相注入
    private final ProductDAO productDAO;

    public BrandService(BrandDAO brandDAO, ProductDAO productDAO) {
        super(brandDAO);
        this.brandDAO = brandDAO;
        this.productDAO = productDAO;
    }

    @Override
    protected void beforeCreate(Brand request) {
        if (brandDAO.existsByCode(request.getCode())) {
            throw new ConflictException("品牌代碼已存在（含已刪除）：" + request.getCode());
        }
    }

    @Override
    protected void beforeUpdate(Brand entity, Brand request) {
        if (request.getCode() != null && !request.getCode().equals(entity.getCode())) {
            throw new BusinessException("品牌代碼建立後不可修改");
        }
    }

    @Override
    protected void beforeDelete(Brand entity) {
        if (productDAO.existsByBrandCodeAndDbMark(entity.getCode(), Product.VALID)) {
            throw new ConflictException("仍有商品使用此品牌：" + entity.getCode());
        }
    }
}
