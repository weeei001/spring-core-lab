package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.TaskComment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 任務留言查詢條件；沒有 project:admin 的人一定要指定 taskId，web-server 依任務所屬專案檢查成員資格。 */
@Getter
@Setter
public class TaskCommentQueryParameter extends QueryParameter<TaskComment> {

    private Long taskId;

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;
}
