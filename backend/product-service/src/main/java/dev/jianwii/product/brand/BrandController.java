package dev.jianwii.product.brand;

import dev.jianwii.api.product.BrandApi;
import dev.jianwii.api.product.BrandQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.product.Brand;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link BrandApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class BrandController implements BrandApi {
    private final BrandService brandService;

    @Override
    public List<Brand> query(BrandQueryParameter parameter) {
        return brandService.query(parameter);
    }

    @Override
    public PageResponse<Brand> queryPage(BrandQueryParameter parameter) {
        return brandService.queryPage(parameter);
    }

    @Override
    public Brand create(Brand brand) {
        return brandService.create(brand);
    }

    @Override
    public Brand update(Long id, Brand brand) {
        return brandService.update(id, brand);
    }

    @Override
    public void delete(Long id) {
        brandService.delete(id);
    }
}
