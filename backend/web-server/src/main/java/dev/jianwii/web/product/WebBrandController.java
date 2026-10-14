package dev.jianwii.web.product;

import dev.jianwii.api.product.BrandApi;
import dev.jianwii.api.product.BrandQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.product.Brand;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的品牌維護：呼叫 product-service 的 {@link BrandApi}，結果包成 Result。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/brand")
public class WebBrandController {
    private final BrandApi brandApi;

    @PreAuthorize("hasAuthority('product:read')")
    @PostMapping("/query")
    public Result<List<Brand>> query(@Valid @RequestBody BrandQueryParameter parameter) {
        return Result.success(brandApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('product:read')")
    @PostMapping("/queryPage")
    public Result<PageResponse<Brand>> queryPage(
            @Valid @RequestBody BrandQueryParameter parameter) {
        return Result.success(brandApi.queryPage(parameter));
    }

    @PreAuthorize("hasAuthority('brand:write')")
    @PostMapping
    public Result<Brand> create(@Validated(ValidGroup.Create.class) @RequestBody Brand brand) {
        return Result.success(brandApi.create(brand));
    }

    @PreAuthorize("hasAuthority('brand:write')")
    @PatchMapping("/{id}")
    public Result<Brand> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Brand brand) {
        return Result.success(brandApi.update(id, brand));
    }

    @PreAuthorize("hasAuthority('brand:write')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        brandApi.delete(id);
        return Result.success();
    }
}
