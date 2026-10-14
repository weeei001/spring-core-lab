package dev.jianwii.api.product;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.product.Tag;
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
 * product-service 的標籤 API（Spring HTTP Interface），用法同 {@link ProductApi}。
 * 商品以 tagIds 關聯標籤（多對多）。
 */
@HttpExchange("/api/tag")
public interface TagApi {

    /** 不分頁查詢（例如下拉選單、依 ids 換成標籤名稱）：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<Tag> query(@Valid @RequestBody TagQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<Tag> queryPage(@Valid @RequestBody TagQueryParameter parameter);

    @PostExchange
    Tag create(@Validated(ValidGroup.Create.class) @RequestBody Tag tag);

    /** 部分更新：只更新有給值（非 null）的欄位。 */
    @PatchExchange("/{id}")
    Tag update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Tag tag);

    /** 軟刪除；仍有商品使用此標籤時回 409。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
