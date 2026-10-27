package dev.jianwii.api.project.api;

import dev.jianwii.api.project.dto.ManualQaBatchQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.ManualQaBatch;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** project-service 的手動驗收批次 API；回傳的批次附帶項目統計。 */
@HttpExchange("/api/manual-qa-batch")
public interface ManualQaBatchApi {

    /** 分頁查詢，未指定排序時新批次在前。 */
    @PostExchange("/queryPage")
    PageResponse<ManualQaBatch> queryPage(@Valid @RequestBody ManualQaBatchQueryParameter parameter);

    @GetExchange("/{id}")
    ManualQaBatch get(@PathVariable("id") Long id);

    @PostExchange
    ManualQaBatch create(@Validated(ValidGroup.Create.class) @RequestBody ManualQaBatch batch);

    /** projectId 不可修改。 */
    @PatchExchange("/{id}")
    ManualQaBatch update(
            @PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaBatch batch);

    /** 以新的版本與環境複製項目，不複製執行紀錄。 */
    @PostExchange("/{id}/copy")
    ManualQaBatch copy(
            @PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaBatch batch);
}
