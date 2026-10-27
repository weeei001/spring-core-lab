package dev.jianwii.project.controller;

import dev.jianwii.api.project.api.ManualQaItemApi;
import dev.jianwii.api.project.dto.ManualQaItemQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.ManualQaItem;
import dev.jianwii.project.service.ManualQaItemService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/** 路徑、參數與驗證註解都寫在 {@link ManualQaItemApi} 介面上，這裡只要轉呼叫 Service。 */
@RestController
@AllArgsConstructor
public class ManualQaItemController implements ManualQaItemApi {
    private final ManualQaItemService manualQaItemService;

    @Override
    public PageResponse<ManualQaItem> queryPage(ManualQaItemQueryParameter parameter) {
        return manualQaItemService.queryPage(parameter);
    }

    @Override
    public ManualQaItem get(Long id) {
        return manualQaItemService.get(id);
    }

    @Override
    public ManualQaItem create(ManualQaItem item) {
        return manualQaItemService.create(item);
    }

    @Override
    public ManualQaItem update(Long id, ManualQaItem item) {
        return manualQaItemService.update(id, item);
    }

    @Override
    public void delete(Long id) {
        manualQaItemService.delete(id);
    }
}
