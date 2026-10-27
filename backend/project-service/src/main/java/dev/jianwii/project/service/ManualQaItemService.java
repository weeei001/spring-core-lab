package dev.jianwii.project.service;

import dev.jianwii.api.project.ManualQaItemQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.ManualQaBatch;
import dev.jianwii.entity.project.ManualQaExecution;
import dev.jianwii.entity.project.ManualQaItem;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskType;
import dev.jianwii.project.dao.ManualQaBatchDAO;
import dev.jianwii.project.dao.ManualQaExecutionDAO;
import dev.jianwii.project.dao.ManualQaItemDAO;
import dev.jianwii.project.dao.TaskDAO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 驗收項目；已有執行紀錄的項目只能改負責人，也不能刪除。 */
@Service
public class ManualQaItemService extends BaseService<ManualQaItem, ManualQaItemQueryParameter> {
    /** 可用 clearFields 清空的欄位。 */
    private static final Set<String> CLEARABLE = Set.of("taskId", "assigneeId");

    /** 有執行紀錄後不可修改的欄位。 */
    private static final Set<String> LOCKED = Set.of("title", "steps", "expectedResult", "taskId");

    private final ManualQaBatchDAO batchDAO;
    private final ManualQaExecutionDAO executionDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;

    public ManualQaItemService(
            ManualQaItemDAO itemDAO,
            ManualQaBatchDAO batchDAO,
            ManualQaExecutionDAO executionDAO,
            TaskDAO taskDAO,
            ProjectRules projectRules) {
        super(itemDAO);
        this.batchDAO = batchDAO;
        this.executionDAO = executionDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManualQaItem> queryPage(ManualQaItemQueryParameter parameter) {
        PageResponse<ManualQaItem> page = super.queryPage(parameter);
        withLatest(page.getItems());
        return page;
    }

    @Transactional(readOnly = true)
    public ManualQaItem get(Long id) {
        ManualQaItem item = find(id);
        withLatest(List.of(item));
        return item;
    }

    @Override
    public ManualQaItem create(ManualQaItem request) {
        ManualQaItem saved = super.create(request);
        withLatest(List.of(saved));
        return saved;
    }

    @Override
    public ManualQaItem update(Long id, ManualQaItem request) {
        ManualQaItem saved = super.update(id, request);
        withLatest(List.of(saved));
        return saved;
    }

    /** 一次查出多個項目的最新紀錄，填入 status 與 latestExecution，避免逐項查詢。 */
    public void withLatest(List<ManualQaItem> items) {
        if (items.isEmpty()) {
            return;
        }
        Map<Long, ManualQaExecution> latest =
                executionDAO
                        .findLatestByItemIds(
                                items.stream().map(ManualQaItem::getId).toList(), BaseEntity.VALID)
                        .stream()
                        .collect(Collectors.toMap(ManualQaExecution::getItemId, Function.identity()));
        for (ManualQaItem item : items) {
            ManualQaExecution execution = latest.get(item.getId());
            item.setLatestExecution(execution);
            item.setStatus(
                    execution == null ? "NOT_TESTED" : execution.getResult().name());
        }
    }

    /** 關聯任務須在同專案；負責人須為專案成員。複製批次時也用來檢查舊項目。 */
    public void requireRelations(Long projectId, Long taskId, String assigneeId) {
        if (taskId != null) {
            requireTask(projectId, taskId, false);
        }
        if (assigneeId != null) {
            projectRules.requireMember(projectId, assigneeId);
        }
    }

    /** 任務須有效且在同專案；bug 為 true 時還須是 BUG 類型。 */
    public Task requireTask(Long projectId, Long taskId, boolean bug) {
        Task task = taskDAO.findById(taskId)
                .filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無任務：" + taskId));
        if (!task.getProjectId().equals(projectId)) {
            throw new BusinessException("任務不屬於此專案：" + taskId);
        }
        if (bug && task.getType() != TaskType.BUG) {
            throw new BusinessException("任務不是 BUG：" + taskId);
        }
        return task;
    }

    /** 項目所屬專案；批次不提供刪除，項目的批次一定有效。 */
    public Long projectOf(ManualQaItem item) {
        return batchDAO.findById(item.getBatchId()).orElseThrow().getProjectId();
    }

    @Override
    protected void beforeCreate(ManualQaItem request) {
        ManualQaBatch batch = batchDAO.findById(request.getBatchId())
                .filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new NotFoundException("查無驗收批次：" + request.getBatchId()));
        projectRules.requireOpen(batch.getProjectId());
        request.setClearFields(null);
        requireRelations(batch.getProjectId(), request.getTaskId(), request.getAssigneeId());
    }

    @Override
    protected void beforeUpdate(ManualQaItem entity, ManualQaItem request) {
        Long projectId = projectOf(entity);
        projectRules.requireOpen(projectId);
        if (request.getBatchId() != null && !request.getBatchId().equals(entity.getBatchId())) {
            throw new BusinessException("項目不可換批次");
        }
        Set<String> clearFields = request.getClearFields() == null ? Set.of() : request.getClearFields();
        if (!CLEARABLE.containsAll(clearFields)) {
            throw new BusinessException("clearFields 只能是 " + CLEARABLE);
        }
        BeanWrapper values = new BeanWrapperImpl(request);
        for (String field : clearFields) {
            if (values.getPropertyValue(field) != null) {
                throw new BusinessException("不能同時給值又清空：" + field);
            }
        }
        if (executionDAO.existsByItemIdAndDbMark(entity.getId(), BaseEntity.VALID)
                && (LOCKED.stream().anyMatch(field -> values.getPropertyValue(field) != null)
                        || clearFields.contains("taskId"))) {
            throw new ConflictException("已執行的項目不可修改內容或任務");
        }
        requireRelations(projectId, request.getTaskId(), request.getAssigneeId());
        request.setBatchId(null);
        request.setStatus(null);
        request.setLatestExecution(null);
    }

    /** 先照一般規則複製有值的欄位，再清空 clearFields 列出的欄位。 */
    @Override
    protected void applyUpdate(ManualQaItem request, ManualQaItem entity) {
        super.applyUpdate(request, entity);
        if (request.getClearFields() != null) {
            BeanWrapper target = new BeanWrapperImpl(entity);
            request.getClearFields().forEach(field -> target.setPropertyValue(field, null));
        }
        entity.setClearFields(null);
    }

    @Override
    protected void beforeDelete(ManualQaItem entity) {
        projectRules.requireOpen(projectOf(entity));
        if (executionDAO.existsByItemIdAndDbMark(entity.getId(), BaseEntity.VALID)) {
            throw new ConflictException("已執行的項目不可刪除");
        }
    }
}
