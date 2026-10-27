package dev.jianwii.project.service;

import dev.jianwii.api.project.WorkLogQueryParameter;
import dev.jianwii.api.project.WorkLogSummary;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.WorkLog;
import dev.jianwii.project.dao.TaskDAO;
import dev.jianwii.project.dao.WorkLogDAO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 工時；只能登記、修改、刪除自己的，同一人同一天合計不超過 24 小時。 */
@Service
public class WorkLogService extends BaseService<WorkLog, WorkLogQueryParameter> {
    private static final BigDecimal UNIT = new BigDecimal("0.5");
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("24");
    private static final int MAX_SUMMARY_DAYS = 366;

    private final WorkLogDAO workLogDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final EntityManager entityManager;

    public WorkLogService(
            WorkLogDAO workLogDAO, TaskDAO taskDAO, ProjectRules projectRules, EntityManager entityManager) {
        super(workLogDAO);
        this.workLogDAO = workLogDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.entityManager = entityManager;
    }

    /**
     * 依人、日合計：GROUP BY 的查詢 Spring Data 的 findAll 做不到，直接用 Criteria API， 篩選條件仍沿用
     * WorkLogQueryParameter（與 queryPage 相同）。
     */
    @Transactional(readOnly = true)
    public List<WorkLogSummary> summary(WorkLogQueryParameter parameter) {
        LocalDate fromDate = parameter.getFromDate();
        LocalDate toDate = parameter.getToDate();
        if (fromDate == null || toDate == null) {
            throw new BusinessException("請指定 fromDate、toDate");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) >= MAX_SUMMARY_DAYS) {
            throw new BusinessException("統計區間最多 " + MAX_SUMMARY_DAYS + " 天");
        }
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<WorkLogSummary> query = builder.createQuery(WorkLogSummary.class);
        Root<WorkLog> root = query.from(WorkLog.class);
        query.select(builder.construct(
                        WorkLogSummary.class,
                        root.get("userId"),
                        root.get("workDate"),
                        builder.sum(root.<BigDecimal>get("hours"))))
                .where(toSpecification(parameter).toPredicate(root, query, builder))
                .groupBy(root.get("userId"), root.get("workDate"))
                .orderBy(builder.asc(root.get("workDate")), builder.asc(root.get("userId")));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    protected void beforeCreate(WorkLog request) {
        Task task = taskDAO.findById(request.getTaskId())
                .orElseThrow(() -> new BusinessException("任務不存在：" + request.getTaskId()));
        projectRules.requireOpen(task.getProjectId());
        request.setProjectId(task.getProjectId());
        request.setUserId(Operator.current());
        requireHours(request.getUserId(), request.getWorkDate(), request.getHours(), 0L);
    }

    @Override
    protected void beforeUpdate(WorkLog entity, WorkLog request) {
        if (request.getTaskId() != null && !request.getTaskId().equals(entity.getTaskId())) {
            throw new BusinessException("taskId 建立後不可修改");
        }
        request.setTaskId(null);
        request.setProjectId(null);
        request.setUserId(null);
        requireOwner(entity);
        projectRules.requireOpen(entity.getProjectId());
        requireHours(
                entity.getUserId(),
                request.getWorkDate() != null ? request.getWorkDate() : entity.getWorkDate(),
                request.getHours() != null ? request.getHours() : entity.getHours(),
                entity.getId());
    }

    @Override
    protected void beforeDelete(WorkLog entity) {
        requireOwner(entity);
        projectRules.requireOpen(entity.getProjectId());
    }

    @Override
    protected Specification<WorkLog> toSpecification(WorkLogQueryParameter parameter) {
        return super.toSpecification(parameter).and(ProjectMemberScope.forUser(parameter.getMemberUserId()));
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "workDate");
    }

    /** 時數以 0.5 小時為單位；加上同一天其他工時（不含 excludeId）不可超過 24 小時。 */
    private void requireHours(String userId, LocalDate workDate, BigDecimal hours, Long excludeId) {
        if (hours.remainder(UNIT).signum() != 0) {
            throw new BusinessException("工時以 0.5 小時為單位");
        }
        BigDecimal logged = workLogDAO.sumHoursOfDay(userId, workDate, excludeId);
        if (logged.add(hours).compareTo(DAILY_LIMIT) > 0) {
            throw new BusinessException(
                    workDate + " 已登記 " + logged.stripTrailingZeros().toPlainString() + " 小時，同一天合計不可超過 24 小時");
        }
    }

    private static void requireOwner(WorkLog workLog) {
        if (!workLog.getUserId().equals(Operator.current())) {
            throw new ForbiddenException("只能修改或刪除自己的工時");
        }
    }
}
