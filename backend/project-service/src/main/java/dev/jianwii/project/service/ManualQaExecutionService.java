package dev.jianwii.project.service;

import dev.jianwii.api.project.ManualQaBugSource;
import dev.jianwii.api.project.ManualQaExecutionQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.ManualQaBatch;
import dev.jianwii.entity.project.ManualQaExecution;
import dev.jianwii.entity.project.ManualQaItem;
import dev.jianwii.entity.project.ManualQaResult;
import dev.jianwii.project.dao.ManualQaBatchDAO;
import dev.jianwii.project.dao.ManualQaExecutionDAO;
import dev.jianwii.project.dao.ManualQaItemDAO;
import dev.jianwii.project.dao.TaskDAO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 驗收紀錄；不可刪除，建立後只能修改失敗紀錄的 BUG 連結。 */
@Service
public class ManualQaExecutionService extends BaseService<ManualQaExecution, ManualQaExecutionQueryParameter> {
    private final ManualQaExecutionDAO executionDAO;
    private final ManualQaItemDAO itemDAO;
    private final ManualQaBatchDAO batchDAO;
    private final TaskDAO taskDAO;
    private final ManualQaItemService itemService;
    private final ProjectRules projectRules;

    public ManualQaExecutionService(
            ManualQaExecutionDAO executionDAO,
            ManualQaItemDAO itemDAO,
            ManualQaBatchDAO batchDAO,
            TaskDAO taskDAO,
            ManualQaItemService itemService,
            ProjectRules projectRules) {
        super(executionDAO);
        this.executionDAO = executionDAO;
        this.itemDAO = itemDAO;
        this.batchDAO = batchDAO;
        this.taskDAO = taskDAO;
        this.itemService = itemService;
        this.projectRules = projectRules;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManualQaExecution> queryPage(ManualQaExecutionQueryParameter parameter) {
        if (parameter.getSort().isEmpty()) {
            QueryParameter.Order executedAt = new QueryParameter.Order();
            executedAt.setField("executedAt");
            executedAt.setDirection("desc");
            QueryParameter.Order id = new QueryParameter.Order();
            id.setField("id");
            id.setDirection("desc");
            parameter.setSort(List.of(executedAt, id));
        }
        return super.queryPage(parameter);
    }

    @Transactional(readOnly = true)
    public ManualQaExecution get(Long id) {
        return find(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ManualQaBugSource> bugSources(Long taskId, int page, int size) {
        taskDAO.findById(taskId).orElseThrow(() -> new NotFoundException("查無任務：" + taskId));
        Sort sort = Sort.by(Sort.Direction.DESC, "executedAt", "id");
        return new PageResponse<>(
                executionDAO.findByBugTaskId(taskId, PageRequest.of(page - 1, size, sort)), execution -> {
                    ManualQaItem item = itemDAO.findById(execution.getItemId()).orElseThrow();
                    ManualQaBatch batch = batchDAO.findById(item.getBatchId()).orElseThrow();
                    return new ManualQaBugSource(
                            execution.getId(),
                            item.getId(),
                            item.getTitle(),
                            batch.getId(),
                            batch.getVersionLabel(),
                            batch.getEnvironment(),
                            execution.getTestedVersion(),
                            execution.getActual(),
                            execution.getExecutedAt());
                });
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("executedAt", "id", "createTime");
    }

    @Override
    protected void beforeCreate(ManualQaExecution request) {
        ManualQaItem item = itemDAO.findById(request.getItemId())
                .orElseThrow(() -> new NotFoundException("查無驗收項目：" + request.getItemId()));
        Long projectId = itemService.projectOf(item);
        projectRules.requireOpen(projectId);
        if (request.getBugTaskId() != null) {
            if (request.getResult() != ManualQaResult.FAIL) {
                throw new BusinessException("只有失敗紀錄可以連結 BUG");
            }
            itemService.requireTask(projectId, request.getBugTaskId(), true);
        }
        request.setTesterId(Operator.current());
        request.setExecutedAt(LocalDateTime.now());
        request.setClearFields(null);
    }

    @Override
    protected void beforeUpdate(ManualQaExecution entity, ManualQaExecution request) {
        Long projectId =
                itemService.projectOf(itemDAO.findById(entity.getItemId()).orElseThrow());
        projectRules.requireOpen(projectId);
        if (entity.getResult() != ManualQaResult.FAIL) {
            throw new BusinessException("只有失敗紀錄可以連結 BUG");
        }
        if (request.getItemId() != null
                || request.getTesterId() != null
                || request.getExecutedAt() != null
                || request.getTestedVersion() != null
                || request.getResult() != null
                || request.getActual() != null) {
            throw new BusinessException("驗收紀錄只能修改 bugTaskId");
        }
        Set<String> clearFields = request.getClearFields() == null ? Set.of() : request.getClearFields();
        if (!Set.of("bugTaskId").containsAll(clearFields)) {
            throw new BusinessException("clearFields 只能是 bugTaskId");
        }
        if ((request.getBugTaskId() == null) == clearFields.isEmpty()) {
            throw new BusinessException("請提供 bugTaskId，或以 clearFields 解除連結");
        }
        if (request.getBugTaskId() != null) {
            itemService.requireTask(projectId, request.getBugTaskId(), true);
        }
    }

    @Override
    protected void applyUpdate(ManualQaExecution request, ManualQaExecution entity) {
        super.applyUpdate(request, entity);
        if (request.getClearFields() != null) {
            entity.setBugTaskId(null);
        }
        entity.setClearFields(null);
    }
}
