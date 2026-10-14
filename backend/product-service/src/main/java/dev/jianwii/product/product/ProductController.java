package dev.jianwii.product.product;

import dev.jianwii.api.product.ProductApi;
import dev.jianwii.api.product.ProductQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.product.Product;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link ProductApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class ProductController implements ProductApi {
    private final ProductService productService;

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
    public void delete(Long id) {
        productService.delete(id);
    }

    @Override
    public List<Product> getByIds(List<Long> ids) {
        return productService.getByIds(ids);
    }

    @Override
    public Product adjustStock(Long id, int quantity) {
        return productService.adjustStock(id, quantity);
    }
}
