package dev.jianwii.api.project.dto;

import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.project.ManualQaBatch;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 驗收批次查詢條件；web-server 依 projectId 檢查成員資格。 */
@Getter
@Setter
public class ManualQaBatchQueryParameter extends QueryParameter<ManualQaBatch> {
    @NotNull
    private Long projectId;
}
