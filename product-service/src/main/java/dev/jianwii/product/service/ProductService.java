package dev.jianwii.product.service;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.product.ProductQueryParameter;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.product.Product;
import dev.jianwii.product.dao.ProductDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService extends BaseService<Product, ProductQueryParameter> {
    private static final int MAX_BATCH = 100;
    /** basic-data 的編號規則代碼，規則由 basic-data 的 SerialNumberSetup 建立。 */
    private static final String SERIAL_KEY = "PRODUCT";

    private final ProductDAO productDAO;
    private final SerialNumberApi serialNumberApi;

    public ProductService(ProductDAO productDAO, SerialNumberApi serialNumberApi) {
        super(productDAO);
        this.productDAO = productDAO;
        this.serialNumberApi = serialNumberApi;
    }

    /** 批次查詢；不存在的 id 直接略過，回傳順序不保證與 ids 相同。 */
    @Transactional(readOnly = true)
    public List<Product> getByIds(List<Long> ids) {
        if (ids.size() > MAX_BATCH) {
            throw new BusinessException("一次最多查詢 " + MAX_BATCH + " 筆");
        }
        return productDAO.findAllById(ids.stream().distinct().toList());
    }

    /** 鎖住該筆商品再計算，同時出庫也不會超賣。 */
    @Transactional
    public Product adjustStock(Long id, int quantity) {
        Product product = productDAO.findWithLockById(id)
                .orElseThrow(() -> new NotFoundException("查無資料，id：" + id));
        long stock = (long) product.getStock() + quantity;
        if (stock < 0) {
            throw new BusinessException("庫存不足，目前庫存：" + product.getStock());
        }
        if (stock > Integer.MAX_VALUE) {
            throw new BusinessException("庫存超出上限");
        }
        product.setStock((int) stock);
        return productDAO.saveAndFlush(product);
    }

    @Override
    protected void beforeCreate(Product request) {
        requireUniqueSku(request.getSku());
        // 檢查都通過才取號，減少跳號
        request.setCode(serialNumberApi.next(SERIAL_KEY));
    }

    @Override
    protected void beforeUpdate(Product entity, Product request) {
        // 系統編號不可修改
        request.setCode(null);
        if (request.getStock() != null) {
            throw new BusinessException("庫存請用調整庫存 API 修改");
        }
        if (request.getSku() != null && !request.getSku().equals(entity.getSku())) {
            requireUniqueSku(request.getSku());
        }
    }

    private void requireUniqueSku(String sku) {
        if (productDAO.existsBySku(sku)) {
            throw new ConflictException("商品貨號已存在：" + sku);
        }
    }
}
