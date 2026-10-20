package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.Sprint;
import dev.jianwii.entity.project.SprintBurndown;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/** project-service 的 Sprint API，用法同 {@link ProjectApi}。 */
@HttpExchange("/api/sprint")
public interface SprintApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。結果帶任務數、完成數與點數合計。 */
    @PostExchange("/query")
    List<Sprint> query(@Valid @RequestBody SprintQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。結果帶任務數、完成數與點數合計。 */
    @PostExchange("/queryPage")
    PageResponse<Sprint> queryPage(@Valid @RequestBody SprintQueryParameter parameter);

    /** 新增；狀態固定為 PLANNED。 */
    @PostExchange
    Sprint create(@Validated(ValidGroup.Create.class) @RequestBody Sprint sprint);

    /** 部分更新：名稱、目標、日期；projectId、status 不可修改，已結束的 Sprint 唯讀。 */
    @PatchExchange("/{id}")
    Sprint update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Sprint sprint);

    /** 軟刪除；進行中的不能刪，排入的任務移回 Backlog。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);

    // ---- 以下為 Sprint 專屬的業務 API ----

    /** 開始 Sprint：必須是 PLANNED，且同專案沒有其他進行中的 Sprint。 */
    @PostExchange("/{id}/start")
    Sprint start(@PathVariable("id") Long id);

    /**
     * 結束 Sprint：必須是 ACTIVE。未完成的任務移到 moveToSprintId（同專案、PLANNED 的 Sprint）， 不給則移回 Backlog。
     */
    @PostExchange("/{id}/close")
    Sprint close(@PathVariable("id") Long id,
            @RequestParam(name = "moveToSprintId", required = false) Long moveToSprintId);

    /** 燃盡圖的每日紀錄，依日期排序；只有有異動的日子才有資料。 */
    @GetExchange("/{id}/burndown")
    List<SprintBurndown> burndown(@PathVariable("id") Long id);
}
