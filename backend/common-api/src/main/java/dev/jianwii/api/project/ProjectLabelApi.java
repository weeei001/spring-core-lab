package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.ProjectLabel;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/** project-service 的專案標籤 API，用法同 {@link ProjectApi}。 */
@HttpExchange("/api/project-label")
public interface ProjectLabelApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。結果帶任務數。 */
    @PostExchange("/query")
    List<ProjectLabel> query(@Valid @RequestBody ProjectLabelQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。結果帶任務數。 */
    @PostExchange("/queryPage")
    PageResponse<ProjectLabel> queryPage(@Valid @RequestBody ProjectLabelQueryParameter parameter);

    /** 同專案已有同名標籤時回 409。 */
    @PostExchange
    ProjectLabel create(@Validated(ValidGroup.Create.class) @RequestBody ProjectLabel label);

    /** 部分更新：只更新有給值（非 null）的欄位；projectId 不可修改。 */
    @PatchExchange("/{id}")
    ProjectLabel update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody ProjectLabel label);

    /** 軟刪除；掛了這個標籤的任務會一併移除它。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
