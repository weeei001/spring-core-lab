package dev.jianwii.api.product;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.product.Product;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * product-service 的 API：成功直接回物件，不包 Result；失敗仍回 Result.failure 與 HTTP 狀態，
 * 由 common 的 RemoteErrorHandler 還原成 RemoteServiceException。
 *
 * <ul>
 * <li>提供端：product-service 的 ProductController implements 此介面。</li>
 * <li>呼叫端：{@code @ImportHttpServices(group = "product-service", types = ProductApi.class)}，
 * 群組名稱即 Eureka 服務名稱。前端經 web-server 的 WebProductController 呼叫，由它包成 Result。</li>
 * </ul>
 */
@HttpExchange("/api/product")
public interface ProductApi {

    /** 依 id 排序，最多 100 筆。 */
    @GetExchange
    List<Product> getAll();

    /** 查無資料時對方回 404，呼叫端收到 RemoteServiceException。 */
    @GetExchange("/{id}")
    Product getById(@PathVariable("id") Long id);

    /** 批次查詢，最多 100 筆；不存在的 id 直接略過。其他服務要顯示多筆商品時用這個，不要逐筆呼叫 getById。 */
    @PostExchange("/batch")
    List<Product> getByIds(@RequestBody List<Long> ids);

    /** 依條件查詢，page 必須為 1、最多 size 筆。 */
    @PostExchange("/query")
    List<Product> query(@Valid @RequestBody ProductQueryParameter parameter);

    @PostExchange("/queryPage")
    PageResponse<Product> queryPage(@Valid @RequestBody ProductQueryParameter parameter);

    @PostExchange
    Product create(@Validated(ValidGroup.Create.class) @RequestBody Product product);

    /** 只更新非 null 欄位；庫存不可在這裡改，請用 {@link #adjustStock}。 */
    @PatchExchange("/{id}")
    Product update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Product product);

    /** 調整庫存：quantity 正數為入庫、負數為出庫；庫存不足時回 400。 */
    @PostExchange("/{id}/stock")
    Product adjustStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
