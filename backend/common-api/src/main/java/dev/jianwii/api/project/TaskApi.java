package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** project-service 的任務 API，用法同 {@link ProjectApi}。 */
@HttpExchange("/api/task")
public interface TaskApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。結果帶子任務數、子任務完成數與已登記工時。 */
    @PostExchange("/query")
    List<Task> query(@Valid @RequestBody TaskQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。結果帶子任務數、子任務完成數與已登記工時。 */
    @PostExchange("/queryPage")
    PageResponse<Task> queryPage(@Valid @RequestBody TaskQueryParameter parameter);

    /**
     * 新增；code 由系統取號，類型不給為 FEATURE、狀態不給為 TODO、優先度不給為 MEDIUM，排在該狀態欄位的最後。 給 parentId
     * 即為子任務：父任務必須同專案，且本身不是子任務。
     */
    @PostExchange
    Task create(@Validated(ValidGroup.Create.class) @RequestBody Task task);

    /**
     * 部分更新：只更新有給值（非 null）的欄位，要清空選填欄位用 clearFields；projectId、parentId 不可修改。 改了狀態會排到新狀態欄位的最後；要指定位置請用
     * {@link #move}。
     */
    @PatchExchange("/{id}")
    Task update(@PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody Task task);

    /** 軟刪除；還有子任務時回 409。其他任務的前置任務若含此任務，一併移除。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);

    // ---- 以下為任務專屬的業務 API ----

    /**
     * 看板拖曳：改成 status，並排在同專案、同狀態的 previousId 之後；previousId 不給表示排第一。
     */
    @PostExchange("/{id}/move")
    Task move(
            @PathVariable("id") Long id,
            @RequestParam("status") TaskStatus status,
            @RequestParam(name = "previousId", required = false) Long previousId);

    /** 操作者（X-Operator）關注任務，之後的狀態變更、新留言會通知他；必須是專案成員。 */
    @PostExchange("/{id}/watch")
    Task watch(@PathVariable("id") Long id);

    /** 操作者取消關注；指派人不論是否關注都會收到通知。 */
    @DeleteExchange("/{id}/watch")
    Task unwatch(@PathVariable("id") Long id);
}
