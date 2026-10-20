package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.TaskActivity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * project-service 的任務活動紀錄 API，用法同 {@link ProjectApi}。
 * 活動紀錄由任務、留言、成員異動時自動寫入，不能從外部新增、修改或刪除，所以只有查詢（基礎五支 API 的唯一例外）。
 */
@HttpExchange("/api/task-activity")
public interface TaskActivityApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<TaskActivity> query(@Valid @RequestBody TaskActivityQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<TaskActivity> queryPage(@Valid @RequestBody TaskActivityQueryParameter parameter);
}
