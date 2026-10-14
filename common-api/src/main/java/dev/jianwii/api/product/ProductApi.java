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
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * product-service 的商品 API（Spring HTTP Interface），提供端與呼叫端共用同一個介面：
 *
 * <ul>
 * <li>提供端：product-service 的 ProductController implements 此介面，這裡的路徑與驗證註解直接生效。</li>
 * <li>呼叫端：啟動類別加 {@code @ImportHttpServices(group = "product-service", types = ProductApi.class)}
 * 後即可注入 ProductApi，Spring 會自動產生實作，呼叫方法就是送出 HTTP 請求；group 即 Eureka 上的服務名稱。
 * 前端經 web-server 的 WebProductController 呼叫，由它包成 Result。</li>
 * </ul>
 *
 * 成功時直接回傳物件，不包 Result；失敗時對方回 Result.failure 與 HTTP 狀態，
 * 呼叫端會收到 RemoteServiceException（轉換在 common 的 RemoteErrorHandler）。
 */
@HttpExchange("/api/product")
public interface ProductApi {

    /** 不分頁查詢（例如下拉選單）：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<Product> query(@Valid @RequestBody ProductQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<Product> queryPage(@Valid @RequestBody ProductQueryParameter parameter);

    /** 新增；只檢查 ValidGroup.Create 與未分組的驗證規則。code 由系統取號，前端給的會被忽略。 */
    @PostExchange
    Product create(@Validated(ValidGroup.Create.class) @RequestBody Product product);

    /** 部分更新：只更新有給值（非 null）的欄位；庫存不可在這裡改，請用 {@link #adjustStock}。 */
    @PatchExchange("/{id}")
    Product update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Product product);

    /** 軟刪除（只標記為已刪除，資料仍在資料表）；不存在或已刪除時回 404。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);

    // ---- 以下為商品專屬的業務 API ----

    /** 批次查詢，最多 100 筆；不存在或已刪除的 id 直接略過。其他服務要顯示多筆商品時用這個。 */
    @PostExchange("/batch")
    List<Product> getByIds(@RequestBody List<Long> ids);

    /** 調整庫存：quantity 正數為入庫、負數為出庫；庫存不足時回 400。 */
    @PostExchange("/{id}/stock")
    Product adjustStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}
