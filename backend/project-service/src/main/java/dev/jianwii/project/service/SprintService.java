package dev.jianwii.project.service;

import dev.jianwii.api.project.SprintQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.Sprint;
import dev.jianwii.entity.project.SprintBurndown;
import dev.jianwii.entity.project.SprintStatus;
import dev.jianwii.entity.project.Task;
import dev.jianwii.project.dao.SprintBurndownDAO;
import dev.jianwii.project.dao.SprintCount;
import dev.jianwii.project.dao.SprintDAO;
import dev.jianwii.project.dao.TaskDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sprint CRUD、開始與結束；查詢結果帶任務與點數合計。 任務排入 Sprint 由 TaskService 處理，這裡只在結束、刪除 Sprint 時整批移動任務。
 */
@Service
public class SprintService extends BaseService<Sprint, SprintQueryParameter> {
    private final SprintDAO sprintDAO;
    private final SprintBurndownDAO sprintBurndownDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final TaskActivityRecorder taskActivityRecorder;
    private final SprintBurndownRecorder sprintBurndownRecorder;

    public SprintService(SprintDAO sprintDAO, SprintBurndownDAO sprintBurndownDAO, TaskDAO taskDAO,
            ProjectRules projectRules, TaskActivityRecorder taskActivityRecorder,
            SprintBurndownRecorder sprintBurndownRecorder) {
        super(sprintDAO);
        this.sprintDAO = sprintDAO;
        this.sprintBurndownDAO = sprintBurndownDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.taskActivityRecorder = taskActivityRecorder;
        this.sprintBurndownRecorder = sprintBurndownRecorder;
    }

    /** 查詢後一次補上這一頁所有 Sprint 的任務與點數合計。 */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<Sprint> queryPage(SprintQueryParameter parameter) {
        PageResponse<Sprint> page = super.queryPage(parameter);
        List<Sprint> sprints = page.getItems();
        if (!sprints.isEmpty()) {
            Map<Long, SprintCount> counts = taskDAO
                    .sprintCounts(sprints.stream().map(Sprint::getId).toList()).stream()
                    .collect(Collectors.toMap(SprintCount::getSprintId, Function.identity()));
            for (Sprint sprint : sprints) {
                SprintCount count = counts.get(sprint.getId());
                sprint.setTaskCount(count == null ? 0L : count.getTotal());
                sprint.setDoneTaskCount(count == null ? 0L : count.getDone());
                sprint.setTotalPoints(count == null ? 0L : count.getPoints());
                sprint.setDonePoints(count == null ? 0L : count.getDonePoints());
            }
        }
        return page;
    }

    /** 開始 Sprint，並記下燃盡圖的第一筆。 */
    public Sprint start(Long id) {
        Sprint sprint = find(id);
        projectRules.requireOpen(sprint.getProjectId());
        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new BusinessException("只有規劃中的 Sprint 可以開始");
        }
        if (sprintDAO.existsByProjectIdAndStatusAndDbMark(sprint.getProjectId(),
                SprintStatus.ACTIVE, BaseEntity.VALID)) {
            throw new ConflictException("已有進行中的 Sprint，請先結束");
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        Sprint saved = sprintDAO.saveAndFlush(sprint);
        sprintBurndownRecorder.refresh(saved.getId());
        return saved;
    }

    /**
     * 結束 Sprint：先記下最後一筆燃盡紀錄，再把未完成的任務移到 moveToSprintId（null 為 Backlog）， 移動寫進各任務的活動紀錄。
     */
    public Sprint close(Long id, Long moveToSprintId) {
        Sprint sprint = find(id);
        projectRules.requireOpen(sprint.getProjectId());
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new BusinessException("只有進行中的 Sprint 可以結束");
        }
        if (moveToSprintId != null) {
            Sprint target = find(moveToSprintId);
            if (!target.getProjectId().equals(sprint.getProjectId())
                    || target.getStatus() != SprintStatus.PLANNED) {
                throw new BusinessException("未完成的任務只能移到同專案、規劃中的 Sprint");
            }
        }
        sprintBurndownRecorder.refresh(id);
        List<Task> unfinished = taskDAO.findBySprintIdAndDbMark(id, BaseEntity.VALID).stream()
                .filter(task -> !task.getStatus().isFinished()).toList();
        moveTasks(unfinished, moveToSprintId);
        sprint.setStatus(SprintStatus.CLOSED);
        return sprintDAO.saveAndFlush(sprint);
    }

    @Transactional(readOnly = true)
    public List<SprintBurndown> burndown(Long id) {
        find(id);
        return sprintBurndownDAO.findBySprintIdOrderByDayAsc(id);
    }

    @Override
    protected void beforeCreate(Sprint request) {
        projectRules.requireOpen(request.getProjectId());
        request.setStatus(SprintStatus.PLANNED);
        requireDateRange(request.getStartDate(), request.getEndDate());
    }

    @Override
    protected void beforeUpdate(Sprint entity, Sprint request) {
        request.setProjectId(null);
        // 狀態只能用 start、close 改
        request.setStatus(null);
        projectRules.requireOpen(entity.getProjectId());
        if (entity.getStatus() == SprintStatus.CLOSED) {
            throw new BusinessException("已結束的 Sprint 不能修改");
        }
        requireDateRange(
                request.getStartDate() != null ? request.getStartDate() : entity.getStartDate(),
                request.getEndDate() != null ? request.getEndDate() : entity.getEndDate());
    }

    /** 進行中的不能刪；排入的任務（含已完成的）移回 Backlog。 */
    @Override
    protected void beforeDelete(Sprint entity) {
        projectRules.requireOpen(entity.getProjectId());
        if (entity.getStatus() == SprintStatus.ACTIVE) {
            throw new BusinessException("進行中的 Sprint 不能刪除，請先結束");
        }
        moveTasks(taskDAO.findBySprintIdAndDbMark(entity.getId(), BaseEntity.VALID), null);
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "startDate", "endDate", "name");
    }

    private void moveTasks(List<Task> tasks, Long sprintId) {
        for (Task task : tasks) {
            taskActivityRecorder.changed(task.getId(), "sprintId", task.getSprintId(), sprintId);
            task.setSprintId(sprintId);
            task.setTransTime(LocalDateTime.now());
        }
        taskDAO.saveAll(tasks);
    }

    private static void requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("開始日不可晚於結束日");
        }
    }
}
