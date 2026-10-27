package dev.jianwii.project.controller;

import dev.jianwii.api.project.ManualQaApi;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.project.service.ManualQaService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ManualQaController implements ManualQaApi {
    private final ManualQaService service;

    @Override
    public PageResponse<BatchView> queryBatches(Query query) {
        return service.queryBatches(query);
    }

    @Override
    public BatchView getBatch(Long id) {
        return service.getBatch(id);
    }

    @Override
    public BatchView createBatch(BatchInput input) {
        return service.createBatch(input);
    }

    @Override
    public BatchView updateBatch(Long id, BatchInput input) {
        return service.updateBatch(id, input);
    }

    @Override
    public BatchView copyBatch(Long id, BatchInput input) {
        return service.copyBatch(id, input);
    }

    @Override
    public PageResponse<ItemView> items(Long id, Integer page, Integer size) {
        return service.items(id, page, size);
    }

    @Override
    public ItemView createItem(Long id, ItemInput input) {
        return service.createItem(id, input);
    }

    @Override
    public ItemView getItem(Long id) {
        return service.getItem(id);
    }

    @Override
    public ItemView updateItem(Long id, Map<String, Object> input) {
        return service.updateItem(id, input);
    }

    @Override
    public void deleteItem(Long id) {
        service.deleteItem(id);
    }

    @Override
    public PageResponse<ExecutionView> executions(Long id, Integer page, Integer size) {
        return service.executions(id, page, size);
    }

    @Override
    public ExecutionView createExecution(Long id, ExecutionInput input) {
        return service.createExecution(id, input);
    }

    @Override
    public ExecutionView getExecution(Long id) {
        return service.getExecution(id);
    }

    @Override
    public ExecutionView updateExecutionBug(Long id, Map<String, Object> input) {
        return service.updateExecutionBug(id, input);
    }
}
