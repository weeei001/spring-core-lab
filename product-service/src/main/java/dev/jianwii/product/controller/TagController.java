package dev.jianwii.product.controller;

import dev.jianwii.api.product.TagApi;
import dev.jianwii.api.product.TagQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.product.Tag;
import dev.jianwii.product.service.TagService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link TagApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class TagController implements TagApi {
    private final TagService tagService;

    @Override
    public List<Tag> query(TagQueryParameter parameter) {
        return tagService.query(parameter);
    }

    @Override
    public PageResponse<Tag> queryPage(TagQueryParameter parameter) {
        return tagService.queryPage(parameter);
    }

    @Override
    public Tag create(Tag tag) {
        return tagService.create(tag);
    }

    @Override
    public Tag update(Long id, Tag tag) {
        return tagService.update(id, tag);
    }

    @Override
    public void delete(Long id) {
        tagService.delete(id);
    }
}
