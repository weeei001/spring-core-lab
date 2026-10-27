package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.ManualQaExecution;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** project-service 的驗收紀錄 API；紀錄不可刪除，建立後只能修改 BUG 連結。 */
@HttpExchange("/api/manual-qa-execution")
public interface ManualQaExecutionApi {

    /** 分頁查詢，未指定排序時新紀錄在前。 */
    @PostExchange("/queryPage")
    PageResponse<ManualQaExecution> queryPage(@Valid @RequestBody ManualQaExecutionQueryParameter parameter);

    @GetExchange("/{id}")
    ManualQaExecution get(@PathVariable("id") Long id);

    /** 執行人與時間由系統填入。 */
    @PostExchange
    ManualQaExecution create(@Validated(ValidGroup.Create.class) @RequestBody ManualQaExecution execution);

    /** 只接受 bugTaskId，或以 clearFields 解除連結。 */
    @PatchExchange("/{id}")
    ManualQaExecution update(
            @PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody ManualQaExecution execution);

    /** 連結到此 BUG 的失敗紀錄，附帶項目與批次資訊。 */
    @GetExchange("/bug/{taskId}")
    PageResponse<ManualQaBugSource> bugSources(
            @PathVariable("taskId") Long taskId,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size);
}
