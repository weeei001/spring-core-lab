package dev.jianwii.api.project.api;

import dev.jianwii.api.project.dto.ManualQaItemQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.ManualQaItem;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** project-service 的驗收項目 API；回傳的項目附帶最新結果。 */
@HttpExchange("/api/manual-qa-item")
public interface ManualQaItemApi {

    @PostExchange("/queryPage")
    PageResponse<ManualQaItem> queryPage(@Valid @RequestBody ManualQaItemQueryParameter parameter);

    @GetExchange("/{id}")
    ManualQaItem get(@PathVariable("id") Long id);

    @PostExchange
    ManualQaItem create(@Validated(ValidGroup.Create.class) @RequestBody ManualQaItem item);

    /** 已執行的項目只能改負責人；以 clearFields 清空 taskId、assigneeId。 */
    @PatchExchange("/{id}")
    ManualQaItem update(
            @PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaItem item);

    /** 只能刪除尚未執行的項目（軟刪除）。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
