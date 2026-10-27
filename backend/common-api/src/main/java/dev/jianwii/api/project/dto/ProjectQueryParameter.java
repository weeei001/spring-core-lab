package dev.jianwii.api.project.dto;

import dev.jianwii.common.query.QueryConditions;
import dev.jianwii.common.query.QueryField;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryOperator;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.Project;
import dev.jianwii.entity.project.ProjectRole;
import dev.jianwii.entity.project.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 專案查詢條件，寫法說明見 {@link QueryParameter}。
 */
@Getter
@Setter
public class ProjectQueryParameter extends QueryParameter<Project> {

    @QueryField(property = "id", operator = QueryOperator.IN)
    @Size(max = 100)
    private List<@NotNull Long> ids;

    /** code 或 name 任一包含關鍵字；不可全為空白。 */
    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String keyword;

    @QueryField(property = "status", operator = QueryOperator.IN)
    @Size(max = 10)
    private List<@NotNull ProjectStatus> statuses;

    /**
     * 只查此使用者是成員的專案。web-server 對沒有 project:admin 的人一律設為登入者，前端給的會被覆寫。 成員在另一張表，條件清單表達不了，由
     * ProjectService.toSpecification 處理。
     */
    @QueryIgnore
    @Size(max = 100)
    private String memberUserId;

    /** 搭配 memberUserId：只查此使用者擔任這個角色的專案，例如「我是經理」。 */
    @QueryIgnore
    private ProjectRole memberRole;

    @Override
    public QueryConditions toQueryConditions() {
        return super.toQueryConditions().keyword(keyword, "code", "name");
    }
}
