package dev.jianwii.project.controller;

import dev.jianwii.api.project.api.ManualQaBatchApi;
import dev.jianwii.api.project.dto.ManualQaBatchQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.ManualQaBatch;
import dev.jianwii.project.service.ManualQaBatchService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/** 路徑、參數與驗證註解都寫在 {@link ManualQaBatchApi} 介面上，這裡只要轉呼叫 Service。 */
@RestController
@AllArgsConstructor
public class ManualQaBatchController implements ManualQaBatchApi {
    private final ManualQaBatchService manualQaBatchService;

    @Override
    public PageResponse<ManualQaBatch> queryPage(ManualQaBatchQueryParameter parameter) {
        return manualQaBatchService.queryPage(parameter);
    }

    @Override
    public ManualQaBatch get(Long id) {
        return manualQaBatchService.get(id);
    }

    @Override
    public ManualQaBatch create(ManualQaBatch batch) {
        return manualQaBatchService.create(batch);
    }

    @Override
    public ManualQaBatch update(Long id, ManualQaBatch batch) {
        return manualQaBatchService.update(id, batch);
    }

    @Override
    public ManualQaBatch copy(Long id, ManualQaBatch batch) {
        return manualQaBatchService.copy(id, batch);
    }
}
