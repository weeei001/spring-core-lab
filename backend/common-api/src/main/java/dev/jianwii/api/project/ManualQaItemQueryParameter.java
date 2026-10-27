package dev.jianwii.api.project;

import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.ManualQaItem;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 驗收項目查詢條件；web-server 依批次所屬專案檢查成員資格。 */
@Getter
@Setter
public class ManualQaItemQueryParameter extends QueryParameter<ManualQaItem> {
    @NotNull
    private Long batchId;
}
