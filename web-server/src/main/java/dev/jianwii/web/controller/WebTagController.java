package dev.jianwii.web.controller;

import dev.jianwii.api.product.TagApi;
import dev.jianwii.api.product.TagQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.product.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的標籤維護：呼叫 product-service 的 {@link TagApi}，結果包成 Result。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/tag")
public class WebTagController {
    private final TagApi tagApi;

    @PostMapping("/query")
    public Result<List<Tag>> query(@Valid @RequestBody TagQueryParameter parameter) {
        return Result.success(tagApi.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<Tag>> queryPage(
            @Valid @RequestBody TagQueryParameter parameter) {
        return Result.success(tagApi.queryPage(parameter));
    }

    @PostMapping
    public Result<Tag> create(@Validated(ValidGroup.Create.class) @RequestBody Tag tag) {
        return Result.success(tagApi.create(tag));
    }

    @PatchMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Tag tag) {
        return Result.success(tagApi.update(id, tag));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagApi.delete(id);
        return Result.success();
    }
}
