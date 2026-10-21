package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.TaskComment;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** project-service 的任務留言 API，用法同 {@link ProjectApi}。 */
@HttpExchange("/api/task-comment")
public interface TaskCommentApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<TaskComment> query(@Valid @RequestBody TaskCommentQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<TaskComment> queryPage(@Valid @RequestBody TaskCommentQueryParameter parameter);

    /** 新增留言，同時寫一筆活動紀錄。 */
    @PostExchange
    TaskComment create(@Validated(ValidGroup.Create.class) @RequestBody TaskComment comment);

    /** 只有作者能修改；taskId 不可修改。 */
    @PatchExchange("/{id}")
    TaskComment update(
            @PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody TaskComment comment);

    /** 只有作者能刪除（軟刪除）。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
