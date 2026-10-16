package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.Sprint;
import dev.jianwii.entity.project.SprintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Sprint 查詢條件，寫法說明見 {@link QueryParameter}。
 * 沒有 project:admin 的人，web-server 會要求給 projectId 或 projectIds，且都必須是自己參與的專案。
 */
@Getter
@Setter
public class SprintQueryParameter extends QueryParameter<Sprint> {

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    private Long projectId;

    @QueryField(property = "projectId", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> projectIds;

    @QueryField(property = "status", operator = QueryOperator.IN)
    @Size(max = 3)
    private List<@NotNull SprintStatus> statuses;
}
