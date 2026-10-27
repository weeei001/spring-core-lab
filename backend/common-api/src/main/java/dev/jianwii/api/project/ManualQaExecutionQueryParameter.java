package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.ManualQaExecution;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 驗收紀錄查詢條件；web-server 依項目所屬專案檢查成員資格。 */
@Getter
@Setter
public class ManualQaExecutionQueryParameter extends QueryParameter<ManualQaExecution> {
    @NotNull
    private Long itemId;
}
