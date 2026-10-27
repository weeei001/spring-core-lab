package dev.jianwii.project.service;

import dev.jianwii.api.project.ManualQaBatchQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.ManualQaBatch;
import dev.jianwii.entity.project.ManualQaItem;
import dev.jianwii.project.dao.ManualQaBatchDAO;
import dev.jianwii.project.dao.ManualQaItemDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 驗收批次；回傳前依各項目最新結果填入統計。批次不提供刪除。 */
@Service
public class ManualQaBatchService extends BaseService<ManualQaBatch, ManualQaBatchQueryParameter> {
    private final ManualQaItemDAO itemDAO;
    private final ManualQaItemService itemService;
    private final ProjectRules projectRules;

    public ManualQaBatchService(
            ManualQaBatchDAO batchDAO,
            ManualQaItemDAO itemDAO,
            ManualQaItemService itemService,
            ProjectRules projectRules) {
        super(batchDAO);
        this.itemDAO = itemDAO;
        this.itemService = itemService;
        this.projectRules = projectRules;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManualQaBatch> queryPage(ManualQaBatchQueryParameter parameter) {
        if (parameter.getSort().isEmpty()) {
            QueryParameter.Order order = new QueryParameter.Order();
            order.setField("id");
            order.setDirection("desc");
            parameter.setSort(List.of(order));
        }
        return super.queryPage(parameter);
    }

    @Transactional(readOnly = true)
    public ManualQaBatch get(Long id) {
        ManualQaBatch batch = find(id);
        initialize(batch);
        return batch;
    }

    /** 以新的版本與環境複製有效項目；任一項目的任務或負責人已失效時整批不複製。 */
    public ManualQaBatch copy(Long id, ManualQaBatch request) {
        ManualQaBatch source = find(id);
        projectRules.requireOpen(source.getProjectId());
        if (request.getProjectId() != null && !request.getProjectId().equals(source.getProjectId())) {
            throw new BusinessException("批次不可複製到其他專案");
        }
        if (request.getVersionLabel() == null || request.getEnvironment() == null) {
            throw new BusinessException("請填寫版本與環境");
        }
        List<ManualQaItem> sourceItems = itemDAO.findByBatchIdOrderByIdAsc(id);
        List<String> invalid = new ArrayList<>();
        for (ManualQaItem item : sourceItems) {
            try {
                itemService.requireRelations(source.getProjectId(), item.getTaskId(), item.getAssigneeId());
            } catch (BusinessException | NotFoundException e) {
                invalid.add(item.getId() + "：" + e.getMessage());
            }
        }
        if (!invalid.isEmpty()) {
            throw new BusinessException("無法複製項目：" + String.join("；", invalid));
        }
        ManualQaBatch result = new ManualQaBatch();
        result.setProjectId(source.getProjectId());
        result.setVersionLabel(request.getVersionLabel());
        result.setEnvironment(request.getEnvironment());
        dao.saveAndFlush(result);
        for (ManualQaItem item : sourceItems) {
            ManualQaItem copy = new ManualQaItem();
            copy.setBatchId(result.getId());
            copy.setTitle(item.getTitle());
            copy.setSteps(item.getSteps());
            copy.setExpectedResult(item.getExpectedResult());
            copy.setTaskId(item.getTaskId());
            copy.setAssigneeId(item.getAssigneeId());
            itemDAO.save(copy);
        }
        itemDAO.flush();
        initialize(result);
        return result;
    }

    @Override
    protected void beforeCreate(ManualQaBatch request) {
        projectRules.requireOpen(request.getProjectId());
    }

    @Override
    protected void beforeUpdate(ManualQaBatch entity, ManualQaBatch request) {
        projectRules.requireOpen(entity.getProjectId());
        if (request.getProjectId() != null && !request.getProjectId().equals(entity.getProjectId())) {
            throw new BusinessException("批次不可變更專案");
        }
        request.setProjectId(null);
    }

    /** 依各項目最新一筆紀錄統計；未測不寫紀錄，所以未測 = 總數 − 有紀錄的項目。 */
    @Override
    protected void initialize(ManualQaBatch batch) {
        List<ManualQaItem> items = itemDAO.findByBatchIdOrderByIdAsc(batch.getId());
        itemService.withLatest(items);
        Map<String, Long> counts =
                items.stream().collect(Collectors.groupingBy(ManualQaItem::getStatus, Collectors.counting()));
        long total = items.size();
        long passed = counts.getOrDefault("PASS", 0L);
        batch.setTotal(total);
        batch.setNotTested(counts.getOrDefault("NOT_TESTED", 0L));
        batch.setPassed(passed);
        batch.setFailed(counts.getOrDefault("FAIL", 0L));
        batch.setBlocked(counts.getOrDefault("BLOCKED", 0L));
        batch.setPassRate(total == 0 ? null : (double) passed / total);
    }
}
