package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ManualQaApi {
    record Query(Long projectId, Integer page, Integer size) {}

    record BatchInput(Long projectId, String versionLabel, String environment) {}

    record ItemInput(String title, String steps, String expectedResult, Long taskId, String assigneeId) {}

    record ExecutionInput(String testedVersion, String result, String actual, Long bugTaskId) {}

    record BatchView(
            Long id,
            Long projectId,
            String versionLabel,
            String environment,
            long total,
            long notTested,
            long passed,
            long failed,
            long blocked,
            Double passRate) {}

    record ItemView(
            Long id,
            Long batchId,
            String title,
            String steps,
            String expectedResult,
            Long taskId,
            String assigneeId,
            String status,
            ExecutionView latestExecution) {}

    record ExecutionView(
            Long id,
            Long itemId,
            String testerId,
            LocalDateTime executedAt,
            String testedVersion,
            String result,
            String actual,
            Long bugTaskId) {}

    record BugExecutionView(
            Long executionId,
            Long itemId,
            String itemTitle,
            Long batchId,
            String versionLabel,
            String environment,
            String testedVersion,
            String actual,
            LocalDateTime executedAt) {}

    @PostExchange("/api/manual-qa-batch/queryPage")
    PageResponse<BatchView> queryBatches(@RequestBody Query query);

    @GetExchange("/api/manual-qa-batch/{id}")
    BatchView getBatch(@PathVariable("id") Long id);

    @PostExchange("/api/manual-qa-batch")
    BatchView createBatch(@RequestBody BatchInput input);

    @PatchExchange("/api/manual-qa-batch/{id}")
    BatchView updateBatch(@PathVariable("id") Long id, @RequestBody BatchInput input);

    @PostExchange("/api/manual-qa-batch/{id}/copy")
    BatchView copyBatch(@PathVariable("id") Long id, @RequestBody BatchInput input);

    @GetExchange("/api/manual-qa-batch/{id}/items")
    PageResponse<ItemView> items(
            @PathVariable("id") Long id, @RequestParam("page") Integer page, @RequestParam("size") Integer size);

    @PostExchange("/api/manual-qa-batch/{id}/items")
    ItemView createItem(@PathVariable("id") Long id, @RequestBody ItemInput input);

    @GetExchange("/api/manual-qa-item/{id}")
    ItemView getItem(@PathVariable("id") Long id);

    @PatchExchange("/api/manual-qa-item/{id}")
    ItemView updateItem(@PathVariable("id") Long id, @RequestBody Map<String, Object> input);

    @DeleteExchange("/api/manual-qa-item/{id}")
    void deleteItem(@PathVariable("id") Long id);

    @GetExchange("/api/manual-qa-item/{id}/executions")
    PageResponse<ExecutionView> executions(
            @PathVariable("id") Long id, @RequestParam("page") Integer page, @RequestParam("size") Integer size);

    @PostExchange("/api/manual-qa-item/{id}/executions")
    ExecutionView createExecution(@PathVariable("id") Long id, @RequestBody ExecutionInput input);

    @GetExchange("/api/manual-qa-execution/{id}")
    ExecutionView getExecution(@PathVariable("id") Long id);

    @PatchExchange("/api/manual-qa-execution/{id}/bug")
    ExecutionView updateExecutionBug(@PathVariable("id") Long id, @RequestBody Map<String, Object> input);

    @GetExchange("/api/manual-qa-bug/{id}/executions")
    PageResponse<BugExecutionView> bugExecutions(
            @PathVariable("id") Long id, @RequestParam("page") Integer page, @RequestParam("size") Integer size);
}
