package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.ProjectMember;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/** project-service 的專案成員 API，用法同 {@link ProjectApi}。 */
@HttpExchange("/api/project-member")
public interface ProjectMemberApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<ProjectMember> query(@Valid @RequestBody ProjectMemberQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<ProjectMember> queryPage(@Valid @RequestBody ProjectMemberQueryParameter parameter);

    /** 加入成員；userId 必須是有效使用者，且尚未是該專案成員。 */
    @PostExchange
    ProjectMember create(@Validated(ValidGroup.Create.class) @RequestBody ProjectMember member);

    /** 只能改角色；最後一位經理不能降為成員。 */
    @PatchExchange("/{id}")
    ProjectMember update(
            @PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody ProjectMember member);

    /** 移除成員（軟刪除），指派給該成員的未完成任務改為未指派；最後一位經理不能移除。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
