package dev.jianwii.product.controller;

import dev.jianwii.api.product.ProductApi;
import dev.jianwii.api.product.ProductQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.product.Product;
import dev.jianwii.product.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證都定義在 {@link ProductApi}，這裡只實作。
 */
@RestController
@AllArgsConstructor
public class ProductController implements ProductApi {
    private final ProductService productService;

    @Override
    public List<Product> getAll() {
        return productService.getAll();
    }

    @Override
    public Product getById(Long id) {
        return productService.getById(id);
    }

    @Override
    public List<Product> getByIds(List<Long> ids) {
        return productService.getByIds(ids);
    }

    @Override
    public List<Product> query(ProductQueryParameter parameter) {
        return productService.query(parameter);
    }

    @Override
    public PageResponse<Product> queryPage(ProductQueryParameter parameter) {
        return productService.queryPage(parameter);
    }

    @Override
    public Product create(Product product) {
        return productService.create(product);
    }

    @Override
    public Product update(Long id, Product product) {
        return productService.update(id, product);
    }

    @Override
    public Product adjustStock(Long id, int quantity) {
        return productService.adjustStock(id, quantity);
    }

    @Override
    public void delete(Long id) {
        productService.deleteById(id);
    }
}
