package dev.jianwii.web.controller;

import dev.jianwii.api.product.ProductApi;
import dev.jianwii.api.product.ProductQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.product.Product;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的商品 API：呼叫 product-service 的 {@link ProductApi}，結果包成 Result。
 * 這裡先驗證一次，格式錯誤不必送到後端服務；後端回的錯誤會變成 RemoteServiceException，
 * 再由 ExceptionHandleController 轉成同樣的 Result.failure 回前端。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/product")
public class WebProductController {
    private final ProductApi productApi;

    @PostMapping("/query")
    public Result<List<Product>> query(@Valid @RequestBody ProductQueryParameter parameter) {
        return Result.success(productApi.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<Product>> queryPage(
            @Valid @RequestBody ProductQueryParameter parameter) {
        return Result.success(productApi.queryPage(parameter));
    }

    @PostMapping
    public Result<Product> create(
            @Validated(ValidGroup.Create.class) @RequestBody Product product) {
        return Result.success(productApi.create(product));
    }

    @PatchMapping("/{id}")
    public Result<Product> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Product product) {
        return Result.success(productApi.update(id, product));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productApi.delete(id);
        return Result.success();
    }

    @PostMapping("/batch")
    public Result<List<Product>> getByIds(@RequestBody List<Long> ids) {
        // 空清單不必呼叫後端
        if (ids.isEmpty()) {
            return Result.success(List.of());
        }
        return Result.success(productApi.getByIds(ids));
    }

    @PostMapping("/{id}/stock")
    public Result<Product> adjustStock(@PathVariable Long id, @RequestParam int quantity) {
        return Result.success(productApi.adjustStock(id, quantity));
    }
}
