package dev.jianwii.project.controller;

import dev.jianwii.api.project.dto.ManualQaBugSource;
import dev.jianwii.api.project.api.ManualQaExecutionApi;
import dev.jianwii.api.project.dto.ManualQaExecutionQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.ManualQaExecution;
import dev.jianwii.project.service.ManualQaExecutionService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/** 路徑、參數與驗證註解都寫在 {@link ManualQaExecutionApi} 介面上，這裡只要轉呼叫 Service。 */
@RestController
@AllArgsConstructor
public class ManualQaExecutionController implements ManualQaExecutionApi {
    private final ManualQaExecutionService manualQaExecutionService;

    @Override
    public PageResponse<ManualQaExecution> queryPage(ManualQaExecutionQueryParameter parameter) {
        return manualQaExecutionService.queryPage(parameter);
    }

    @Override
    public ManualQaExecution get(Long id) {
        return manualQaExecutionService.get(id);
    }

    @Override
    public ManualQaExecution create(ManualQaExecution execution) {
        return manualQaExecutionService.create(execution);
    }

    @Override
    public ManualQaExecution update(Long id, ManualQaExecution execution) {
        return manualQaExecutionService.update(id, execution);
    }

    @Override
    public PageResponse<ManualQaBugSource> bugSources(Long taskId, Integer page, Integer size) {
        return manualQaExecutionService.bugSources(taskId, page, size);
    }
}
