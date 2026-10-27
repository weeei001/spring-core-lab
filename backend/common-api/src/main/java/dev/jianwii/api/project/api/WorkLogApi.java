package dev.jianwii.api.project.api;

import dev.jianwii.api.project.dto.WorkLogQueryParameter;
import dev.jianwii.api.project.dto.WorkLogSummary;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.WorkLog;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** project-service 的工時 API，用法同 {@link ProjectApi}。 */
@HttpExchange("/api/work-log")
public interface WorkLogApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<WorkLog> query(@Valid @RequestBody WorkLogQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<WorkLog> queryPage(@Valid @RequestBody WorkLogQueryParameter parameter);

    /** 登記工時；userId 為目前操作者、projectId 由任務帶入，前端給的都會被忽略。 */
    @PostExchange
    WorkLog create(@Validated(ValidGroup.Create.class) @RequestBody WorkLog workLog);

    /** 只有登記者能修改；taskId 不可修改。 */
    @PatchExchange("/{id}")
    WorkLog update(@PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody WorkLog workLog);

    /** 只有登記者能刪除（軟刪除）。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);

    // ---- 以下為工時專屬的業務 API ----

    /**
     * 依人、日合計工時，前端再組成週表或圖表；必須給 fromDate、toDate，區間最多 366 天。 page、size、sort 不使用。
     */
    @PostExchange("/summary")
    List<WorkLogSummary> summary(@Valid @RequestBody WorkLogQueryParameter parameter);
}
