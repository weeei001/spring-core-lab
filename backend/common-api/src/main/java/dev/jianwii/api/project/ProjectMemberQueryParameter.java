package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.ProjectMember;
import dev.jianwii.entity.project.ProjectRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 專案成員查詢條件，寫法說明見 {@link QueryParameter}。 沒有 project:admin 的人，web-server 會要求給
 * projectId（且自己是成員），否則只查得到自己的成員資料。
 */
@Getter
@Setter
public class ProjectMemberQueryParameter extends QueryParameter<ProjectMember> {

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    private Long projectId;

    /** IN：例如查自己在多個專案中的角色。 */
    @QueryField(property = "projectId", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> projectIds;

    @Size(max = 100)
    private String userId;

    private ProjectRole role;
}
