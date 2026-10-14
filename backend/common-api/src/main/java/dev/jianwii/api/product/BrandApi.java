package dev.jianwii.api.product;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.product.Brand;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * product-service 的品牌 API（Spring HTTP Interface），用法同 {@link ProductApi}。
 * 商品以 brandCode 關聯品牌（多對一）。
 */
@HttpExchange("/api/brand")
public interface BrandApi {

    /** 不分頁查詢（例如下拉選單、依 codes 換成品牌名稱）：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<Brand> query(@Valid @RequestBody BrandQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<Brand> queryPage(@Valid @RequestBody BrandQueryParameter parameter);

    /** 新增；code 不可與既有品牌重複（含已刪除）。 */
    @PostExchange
    Brand create(@Validated(ValidGroup.Create.class) @RequestBody Brand brand);

    /** 部分更新：只更新有給值（非 null）的欄位；code 不可修改。 */
    @PatchExchange("/{id}")
    Brand update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Brand brand);

    /** 軟刪除；仍有商品使用此品牌時回 409。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
