package dev.jianwii.project.service;

import dev.jianwii.api.project.ManualQaApi.*;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.*;
import dev.jianwii.project.dao.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ManualQaService {
    private final ManualQaBatchDAO batches;
    private final ManualQaItemDAO items;
    private final ManualQaExecutionDAO executions;
    private final ProjectDAO projects;
    private final TaskDAO tasks;
    private final ProjectRules rules;

    @Transactional(readOnly = true)
    public PageResponse<BatchView> queryBatches(Query query) {
        requireProject(query.projectId());
        return new PageResponse<>(
                batches.findByProjectIdAndDbMark(
                        query.projectId(),
                        BaseEntity.VALID,
                        page(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "id"))),
                this::batchView);
    }

    @Transactional(readOnly = true)
    public BatchView getBatch(Long id) {
        return batchView(batch(id));
    }

    public BatchView createBatch(BatchInput input) {
        rules.requireOpen(input.projectId());
        ManualQaBatch batch = new ManualQaBatch();
        batch.setProjectId(input.projectId());
        batch.setVersionLabel(required(input.versionLabel(), "版本", 200, false));
        batch.setEnvironment(required(input.environment(), "環境", 1000, false));
        batch.setDbMark(BaseEntity.VALID);
        return batchView(batches.saveAndFlush(batch));
    }

    public BatchView updateBatch(Long id, BatchInput input) {
        ManualQaBatch batch = batch(id);
        rules.requireOpen(batch.getProjectId());
        if (input.projectId() != null && !input.projectId().equals(batch.getProjectId())) {
            throw new BusinessException("批次不可變更專案");
        }
        if (input.versionLabel() != null) batch.setVersionLabel(required(input.versionLabel(), "版本", 200, false));
        if (input.environment() != null) batch.setEnvironment(required(input.environment(), "環境", 1000, false));
        return batchView(batches.saveAndFlush(batch));
    }

    public BatchView copyBatch(Long id, BatchInput input) {
        ManualQaBatch source = batch(id);
        rules.requireOpen(source.getProjectId());
        if (input.projectId() != null && !input.projectId().equals(source.getProjectId())) {
            throw new BusinessException("批次不可複製到其他專案");
        }
        String version = required(input.versionLabel(), "版本", 200, false);
        String environment = required(input.environment(), "環境", 1000, false);
        List<ManualQaItem> sourceItems = items.findByBatchIdAndDbMarkOrderByIdAsc(id, BaseEntity.VALID);
        List<String> invalid = new ArrayList<>();
        for (ManualQaItem item : sourceItems) {
            try {
                requireRelations(source.getProjectId(), item.getTaskId(), item.getAssigneeId());
            } catch (BusinessException | NotFoundException e) {
                invalid.add(item.getId() + "：" + e.getMessage());
            }
        }
        if (!invalid.isEmpty()) throw new BusinessException("無法複製項目：" + String.join("；", invalid));
        ManualQaBatch result = new ManualQaBatch();
        result.setProjectId(source.getProjectId());
        result.setVersionLabel(version);
        result.setEnvironment(environment);
        result.setDbMark(BaseEntity.VALID);
        batches.saveAndFlush(result);
        for (ManualQaItem item : sourceItems) {
            ManualQaItem copy = new ManualQaItem();
            copy.setBatchId(result.getId());
            copy.setTitle(item.getTitle());
            copy.setSteps(item.getSteps());
            copy.setExpectedResult(item.getExpectedResult());
            copy.setTaskId(item.getTaskId());
            copy.setAssigneeId(item.getAssigneeId());
            copy.setDbMark(BaseEntity.VALID);
            items.save(copy);
        }
        items.flush();
        return batchView(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemView> items(Long id, Integer number, Integer size) {
        batch(id);
        return new PageResponse<>(
                items.findByBatchIdAndDbMark(id, BaseEntity.VALID, page(number, size, Sort.by("id"))), this::itemView);
    }

    public ItemView createItem(Long batchId, ItemInput input) {
        ManualQaBatch batch = batch(batchId);
        rules.requireOpen(batch.getProjectId());
        ManualQaItem item = new ManualQaItem();
        item.setBatchId(batchId);
        item.setTitle(required(input.title(), "標題", 200, false));
        item.setSteps(required(input.steps(), "步驟", 65535, true));
        item.setExpectedResult(required(input.expectedResult(), "預期結果", 65535, true));
        requireRelations(batch.getProjectId(), input.taskId(), input.assigneeId());
        item.setTaskId(input.taskId());
        item.setAssigneeId(input.assigneeId());
        item.setDbMark(BaseEntity.VALID);
        return itemView(items.saveAndFlush(item));
    }

    @Transactional(readOnly = true)
    public ItemView getItem(Long id) {
        return itemView(item(id));
    }

    public ItemView updateItem(Long id, Map<String, Object> input) {
        ManualQaItem item = item(id);
        ManualQaBatch batch = batch(item.getBatchId());
        rules.requireOpen(batch.getProjectId());
        if (!Set.of("title", "steps", "expectedResult", "taskId", "assigneeId").containsAll(input.keySet())) {
            throw new BusinessException("包含不支援的項目欄位");
        }
        if (executions.existsByItemIdAndDbMark(id, BaseEntity.VALID)
                && input.keySet().stream().anyMatch(Set.of("title", "steps", "expectedResult", "taskId")::contains)) {
            throw new ConflictException("已執行的項目不可修改內容或任務");
        }
        if (input.containsKey("title")) item.setTitle(required(string(input.get("title")), "標題", 200, false));
        if (input.containsKey("steps")) item.setSteps(required(string(input.get("steps")), "步驟", 65535, true));
        if (input.containsKey("expectedResult"))
            item.setExpectedResult(required(string(input.get("expectedResult")), "預期結果", 65535, true));
        Long taskId = input.containsKey("taskId") ? id(input.get("taskId")) : item.getTaskId();
        String assignee = input.containsKey("assigneeId") ? string(input.get("assigneeId")) : item.getAssigneeId();
        if (input.containsKey("taskId") && taskId != null) requireTask(batch.getProjectId(), taskId, false);
        if (input.containsKey("assigneeId") && assignee != null) requireAssignee(batch.getProjectId(), assignee);
        item.setTaskId(taskId);
        item.setAssigneeId(assignee);
        return itemView(items.saveAndFlush(item));
    }

    public void deleteItem(Long id) {
        ManualQaItem item = item(id);
        rules.requireOpen(batch(item.getBatchId()).getProjectId());
        if (executions.existsByItemIdAndDbMark(id, BaseEntity.VALID)) throw new ConflictException("已執行的項目不可刪除");
        item.setDbMark(BaseEntity.INVALID);
        items.saveAndFlush(item);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExecutionView> executions(Long itemId, Integer number, Integer size) {
        item(itemId);
        return new PageResponse<>(
                executions.findByItemIdAndDbMark(
                        itemId, BaseEntity.VALID, page(number, size, Sort.by(Sort.Direction.DESC, "executedAt", "id"))),
                this::executionView);
    }

    public ExecutionView createExecution(Long itemId, ExecutionInput input) {
        ManualQaItem item = item(itemId);
        Long projectId = batch(item.getBatchId()).getProjectId();
        rules.requireOpen(projectId);
        if (input.result() == null || !Set.of("PASS", "FAIL", "BLOCKED").contains(input.result()))
            throw new BusinessException("結果必須是 PASS、FAIL 或 BLOCKED");
        if (input.bugTaskId() != null) {
            if (!"FAIL".equals(input.result())) throw new BusinessException("只有失敗紀錄可以連結 BUG");
            requireTask(projectId, input.bugTaskId(), true);
        }
        ManualQaExecution execution = new ManualQaExecution();
        execution.setItemId(itemId);
        execution.setTesterId(required(Operator.current(), "執行人", 100, false));
        execution.setExecutedAt(LocalDateTime.now());
        execution.setTestedVersion(required(input.testedVersion(), "實測版本", 200, false));
        execution.setResult(input.result());
        execution.setActual(required(input.actual(), "實際現象", 65535, true));
        execution.setBugTaskId(input.bugTaskId());
        execution.setDbMark(BaseEntity.VALID);
        return executionView(executions.saveAndFlush(execution));
    }

    @Transactional(readOnly = true)
    public ExecutionView getExecution(Long id) {
        return executionView(execution(id));
    }

    public ExecutionView updateExecutionBug(Long id, Map<String, Object> input) {
        ManualQaExecution execution = execution(id);
        Long projectId = batch(item(execution.getItemId()).getBatchId()).getProjectId();
        rules.requireOpen(projectId);
        if (!"FAIL".equals(execution.getResult())) throw new BusinessException("只有失敗紀錄可以連結 BUG");
        if (!input.keySet().equals(Set.of("bugTaskId"))) throw new BusinessException("必須提供 bugTaskId");
        Long bugId = id(input.get("bugTaskId"));
        if (bugId != null) requireTask(projectId, bugId, true);
        execution.setBugTaskId(bugId);
        return executionView(executions.saveAndFlush(execution));
    }

    private BatchView batchView(ManualQaBatch batch) {
        List<ItemView> views = items.findByBatchIdAndDbMarkOrderByIdAsc(batch.getId(), BaseEntity.VALID).stream()
                .map(this::itemView)
                .toList();
        long passed = views.stream().filter(v -> v.status().equals("PASS")).count();
        long failed = views.stream().filter(v -> v.status().equals("FAIL")).count();
        long blocked = views.stream().filter(v -> v.status().equals("BLOCKED")).count();
        long total = views.size();
        return new BatchView(
                batch.getId(),
                batch.getProjectId(),
                batch.getVersionLabel(),
                batch.getEnvironment(),
                total,
                total - passed - failed - blocked,
                passed,
                failed,
                blocked,
                total == 0 ? null : (double) passed / total);
    }

    private ItemView itemView(ManualQaItem item) {
        ExecutionView latest = executions
                .findFirstByItemIdAndDbMarkOrderByExecutedAtDescIdDesc(item.getId(), BaseEntity.VALID)
                .map(this::executionView)
                .orElse(null);
        return new ItemView(
                item.getId(),
                item.getBatchId(),
                item.getTitle(),
                item.getSteps(),
                item.getExpectedResult(),
                item.getTaskId(),
                item.getAssigneeId(),
                latest == null ? "NOT_TESTED" : latest.result(),
                latest);
    }

    private ExecutionView executionView(ManualQaExecution e) {
        return new ExecutionView(
                e.getId(),
                e.getItemId(),
                e.getTesterId(),
                e.getExecutedAt(),
                e.getTestedVersion(),
                e.getResult(),
                e.getActual(),
                e.getBugTaskId());
    }

    private ManualQaBatch batch(Long id) {
        ManualQaBatch b = batches.findById(id)
                .filter(v -> v.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無驗收批次"));
        requireProject(b.getProjectId());
        return b;
    }

    private ManualQaItem item(Long id) {
        ManualQaItem i = items.findById(id)
                .filter(v -> v.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無驗收項目"));
        batch(i.getBatchId());
        return i;
    }

    private ManualQaExecution execution(Long id) {
        ManualQaExecution e = executions
                .findById(id)
                .filter(v -> v.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無驗收紀錄"));
        item(e.getItemId());
        return e;
    }

    private void requireProject(Long id) {
        if (id == null
                || projects.findById(id)
                        .filter(v -> v.getDbMark() == BaseEntity.VALID)
                        .isEmpty()) throw new NotFoundException("查無專案");
    }

    private void requireRelations(Long projectId, Long taskId, String assignee) {
        if (taskId != null) requireTask(projectId, taskId, false);
        if (assignee != null) requireAssignee(projectId, assignee);
    }

    private void requireAssignee(Long projectId, String assignee) {
        if (!required(assignee, "負責人", 100, false).equals(assignee)) throw new BusinessException("負責人格式錯誤");
        rules.requireMember(projectId, assignee);
    }

    private void requireTask(Long projectId, Long taskId, boolean bug) {
        Task task = tasks.findById(taskId)
                .filter(v -> v.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無任務：" + taskId));
        if (!task.getProjectId().equals(projectId) || (bug && task.getType() != TaskType.BUG))
            throw new BusinessException("任務不屬於此專案或不是 BUG：" + taskId);
    }

    private static String required(String value, String name, int max, boolean bytes) {
        if (value == null || value.isBlank()) throw new BusinessException(name + "不可為空");
        String clean = value.strip();
        if ((bytes ? clean.getBytes(StandardCharsets.UTF_8).length : clean.codePointCount(0, clean.length())) > max)
            throw new BusinessException(name + "超過長度限制");
        return clean;
    }

    private static String string(Object value) {
        if (value == null || value instanceof String) return (String) value;
        throw new BusinessException("文字欄位格式錯誤");
    }

    private static Long id(Object value) {
        if (value == null) return null;
        if (value instanceof Integer number) return number.longValue();
        if (value instanceof Long number) return number;
        throw new BusinessException("ID 格式錯誤");
    }

    private static PageRequest page(Integer number, Integer size, Sort sort) {
        if (number == null
                || size == null
                || number < 1
                || size < 1
                || size > 100
                || (long) (number - 1) * size > Integer.MAX_VALUE)
            throw new BusinessException("page 從 1 起算，size 介於 1 至 100");
        return PageRequest.of(number - 1, size, sort);
    }
}
