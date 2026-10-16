package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.TaskAttachment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 任務附件查詢條件；沒有 project:admin 的人一定要指定 taskId，web-server 依任務所屬專案檢查成員資格。 */
@Getter
@Setter
public class TaskAttachmentQueryParameter extends QueryParameter<TaskAttachment> {

    private Long taskId;

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;
}
