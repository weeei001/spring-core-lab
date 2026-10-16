package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.Milestone;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/** project-service 的里程碑 API，用法同 {@link ProjectApi}。 */
@HttpExchange("/api/milestone")
public interface MilestoneApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。結果帶任務數與完成數。 */
    @PostExchange("/query")
    List<Milestone> query(@Valid @RequestBody MilestoneQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。結果帶任務數與完成數。 */
    @PostExchange("/queryPage")
    PageResponse<Milestone> queryPage(@Valid @RequestBody MilestoneQueryParameter parameter);

    @PostExchange
    Milestone create(@Validated(ValidGroup.Create.class) @RequestBody Milestone milestone);

    /** 部分更新：只更新有給值（非 null）的欄位；projectId 不可修改。 */
    @PatchExchange("/{id}")
    Milestone update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Milestone milestone);

    /** 軟刪除；仍有任務掛在此里程碑時回 409。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
