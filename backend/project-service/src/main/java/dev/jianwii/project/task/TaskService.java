package dev.jianwii.project.task;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.project.TaskQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskPriority;
import dev.jianwii.entity.project.TaskStatus;
import dev.jianwii.project.activity.TaskActivityRecorder;
import dev.jianwii.project.milestone.MilestoneDAO;
import dev.jianwii.project.notification.TaskNotifier;
import dev.jianwii.project.project.ProjectRules;
import dev.jianwii.project.worklog.TaskHours;
import dev.jianwii.project.worklog.WorkLogDAO;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任務 CRUD 與看板排序；子任務、前置任務、指派人、里程碑的檢查，以及欄位變更的活動紀錄。
 */
@Service
public class TaskService extends BaseService<Task, TaskQueryParameter> {
    /** basic-data 的編號規則代碼，規則由 basic-data 的 SerialNumberSetup 建立。 */
    private static final String SERIAL_KEY = "TASK";
    /** 看板排序值的間隔：插到兩張卡片之間時取中間值，間隔用完才整欄重新編號，平常只改被拖的那一筆。 */
    private static final long GAP = 1024;
    /** 可用 clearFields 清空的欄位。 */
    private static final Set<String> CLEARABLE = Set.of("milestoneId", "assigneeId", "startDate",
            "dueDate", "estimateHours", "description");

    private final TaskDAO taskDAO;
    private final MilestoneDAO milestoneDAO;
    private final WorkLogDAO workLogDAO;
    private final ProjectRules projectRules;
    private final TaskActivityRecorder taskActivityRecorder;
    private final TaskNotifier taskNotifier;
    private final SerialNumberApi serialNumberApi;

    public TaskService(TaskDAO taskDAO, MilestoneDAO milestoneDAO, WorkLogDAO workLogDAO,
            ProjectRules projectRules, TaskActivityRecorder taskActivityRecorder,
            TaskNotifier taskNotifier, SerialNumberApi serialNumberApi) {
        super(taskDAO);
        this.taskDAO = taskDAO;
        this.milestoneDAO = milestoneDAO;
        this.workLogDAO = workLogDAO;
        this.projectRules = projectRules;
        this.taskActivityRecorder = taskActivityRecorder;
        this.taskNotifier = taskNotifier;
        this.serialNumberApi = serialNumberApi;
    }

    /** 查詢後一次補上這一頁所有任務的子任務統計與工時合計。 */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<Task> queryPage(TaskQueryParameter parameter) {
        PageResponse<Task> page = super.queryPage(parameter);
        List<Task> tasks = page.getItems();
        if (!tasks.isEmpty()) {
            List<Long> ids = tasks.stream().map(Task::getId).toList();
            Map<Long, TaskCount> subtasks = taskDAO.subtaskCounts(ids).stream()
                    .collect(Collectors.toMap(TaskCount::getGroupId, Function.identity()));
            Map<Long, BigDecimal> hours = workLogDAO.sumHoursByTask(ids).stream()
                    .collect(Collectors.toMap(TaskHours::getTaskId, TaskHours::getHours));
            for (Task task : tasks) {
                if (task.getParentId() == null) {
                    TaskCount count = subtasks.get(task.getId());
                    task.setSubtaskCount(count == null ? 0L : count.getTotal());
                    task.setDoneSubtaskCount(count == null ? 0L : count.getDone());
                }
                task.setLoggedHours(hours.getOrDefault(task.getId(), BigDecimal.ZERO));
            }
        }
        return page;
    }

    @Override
    public Task create(Task request) {
        Task saved = super.create(request);
        taskActivityRecorder.created(saved.getId());
        if (saved.getAssigneeId() != null) {
            taskNotifier.assigned(saved);
        }
        return saved;
    }

    /** 更新後比對新舊值，有變的欄位寫進活動紀錄，並通知相關的人。 */
    @Override
    public Task update(Long id, Task request) {
        Tracked before = Tracked.of(find(id));
        Task saved = super.update(id, request);
        recordChanges(before, saved);
        return saved;
    }

    /**
     * 看板拖曳：改成 status，排在同欄位 previousId 之後（null 表示排第一）。
     * 平常只改這一筆的排序值；前後兩張卡片之間沒有空隙時，整欄重新編號。
     */
    public Task move(Long id, TaskStatus status, Long previousId) {
        Task task = find(id);
        projectRules.requireOpen(task.getProjectId());
        Tracked before = Tracked.of(task);
        List<Task> column = new ArrayList<>(
                taskDAO.findByProjectIdAndStatusAndDbMarkOrderBySortOrderAscIdAsc(
                        task.getProjectId(), status, BaseEntity.VALID));
        column.removeIf(other -> other.getId().equals(id));
        int index = 0;
        if (previousId != null) {
            index = column.stream().map(Task::getId).toList().indexOf(previousId) + 1;
            if (index == 0) {
                throw new BusinessException("previousId 不在目標欄位：" + previousId);
            }
        }
        task.setStatus(status);
        Long previous = index > 0 ? column.get(index - 1).getSortOrder() : null;
        Long next = index < column.size() ? column.get(index).getSortOrder() : null;
        if (previous != null && next != null && next - previous < 2) {
            column.add(index, task);
            for (int i = 0; i < column.size(); i++) {
                column.get(i).setSortOrder((i + 1) * GAP);
            }
            taskDAO.saveAll(column);
        } else if (previous == null && next == null) {
            task.setSortOrder(GAP);
        } else if (previous == null) {
            task.setSortOrder(next - GAP);
        } else if (next == null) {
            task.setSortOrder(previous + GAP);
        } else {
            task.setSortOrder(previous + (next - previous) / 2);
        }
        Task saved = taskDAO.saveAndFlush(task);
        recordChanges(before, saved);
        initialize(saved);
        return saved;
    }

    @Override
    protected void beforeCreate(Task request) {
        Long projectId = request.getProjectId();
        projectRules.requireOpen(projectId);
        request.setClearFields(null);
        if (request.getParentId() != null) {
            Task parent = findValid(request.getParentId())
                    .orElseThrow(() -> new BusinessException("父任務不存在：" + request.getParentId()));
            if (!parent.getProjectId().equals(projectId)) {
                throw new BusinessException("父任務必須在同一個專案");
            }
            if (parent.getParentId() != null) {
                throw new BusinessException("子任務底下不能再有子任務");
            }
        }
        if (request.getMilestoneId() != null) {
            requireMilestone(projectId, request.getMilestoneId());
        }
        if (request.getAssigneeId() != null) {
            projectRules.requireMember(projectId, request.getAssigneeId());
        }
        requireDateRange(request.getStartDate(), request.getDueDate());
        if (request.getPredecessorIds() == null) {
            request.setPredecessorIds(new HashSet<>());
        }
        requirePredecessors(null, projectId, request.getPredecessorIds());
        if (request.getStatus() == null) {
            request.setStatus(TaskStatus.TODO);
        }
        if (request.getPriority() == null) {
            request.setPriority(TaskPriority.MEDIUM);
        }
        request.setSortOrder(nextSortOrder(projectId, request.getStatus()));
        // 號碼取走就不會退回，所以檢查都通過才取號，減少跳號
        request.setCode(serialNumberApi.next(SERIAL_KEY));
    }

    @Override
    protected void beforeUpdate(Task entity, Task request) {
        Long projectId = entity.getProjectId();
        projectRules.requireOpen(projectId);
        // 系統維護的欄位不採用前端的值
        request.setCode(null);
        request.setSortOrder(null);
        requireUnchanged("projectId", request.getProjectId(), projectId);
        request.setProjectId(null);
        requireUnchanged("parentId", request.getParentId(), entity.getParentId());
        request.setParentId(null);

        Set<String> clearFields =
                request.getClearFields() == null ? Set.of() : request.getClearFields();
        if (!CLEARABLE.containsAll(clearFields)) {
            throw new BusinessException("clearFields 只能是 " + CLEARABLE);
        }
        BeanWrapper values = new BeanWrapperImpl(request);
        for (String field : clearFields) {
            if (values.getPropertyValue(field) != null) {
                throw new BusinessException("不能同時給值又清空：" + field);
            }
        }

        if (request.getMilestoneId() != null) {
            requireMilestone(projectId, request.getMilestoneId());
        }
        if (request.getAssigneeId() != null) {
            projectRules.requireMember(projectId, request.getAssigneeId());
        }
        LocalDate startDate = clearFields.contains("startDate") ? null
                : request.getStartDate() != null ? request.getStartDate() : entity.getStartDate();
        LocalDate dueDate = clearFields.contains("dueDate") ? null
                : request.getDueDate() != null ? request.getDueDate() : entity.getDueDate();
        requireDateRange(startDate, dueDate);
        if (request.getPredecessorIds() != null) {
            requirePredecessors(entity.getId(), projectId, request.getPredecessorIds());
            // 只改前置任務時主表不會 UPDATE，強制更新修改者與時間
            touch(entity);
        }
        if (request.getStatus() != null && request.getStatus() != entity.getStatus()) {
            // 換到新狀態欄位的最後
            request.setSortOrder(nextSortOrder(projectId, request.getStatus()));
        }
    }

    /** 先照一般規則複製有值的欄位，再清空 clearFields 列出的欄位。 */
    @Override
    protected void applyUpdate(Task request, Task entity) {
        super.applyUpdate(request, entity);
        if (request.getClearFields() != null) {
            BeanWrapper target = new BeanWrapperImpl(entity);
            request.getClearFields().forEach(field -> target.setPropertyValue(field, null));
        }
        entity.setClearFields(null);
    }

    /** 有子任務時不可刪；其他任務的前置任務若含此任務，一併移除。 */
    @Override
    protected void beforeDelete(Task entity) {
        projectRules.requireOpen(entity.getProjectId());
        if (taskDAO.existsByParentIdAndDbMark(entity.getId(), BaseEntity.VALID)) {
            throw new ConflictException("請先刪除子任務：" + entity.getCode());
        }
        List<Task> dependents =
                taskDAO.findByPredecessorIdsContainingAndDbMark(entity.getId(), BaseEntity.VALID);
        for (Task dependent : dependents) {
            dependent.getPredecessorIds().remove(entity.getId());
            touch(dependent);
        }
        taskDAO.saveAll(dependents);
    }

    /** topLevel：parentId 是否為 null，條件清單表達不了 IS NULL，在這裡補上。 */
    @Override
    protected Specification<Task> toSpecification(TaskQueryParameter parameter) {
        Specification<Task> specification = super.toSpecification(parameter);
        Boolean topLevel = parameter.getTopLevel();
        if (topLevel == null) {
            return specification;
        }
        return specification.and((root, query, builder) -> topLevel
                ? builder.isNull(root.get("parentId")) : builder.isNotNull(root.get("parentId")));
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "code", "title", "sortOrder", "startDate",
                "dueDate");
    }

    /** 回傳前載入前置任務（LAZY 的中間表）。 */
    @Override
    protected void initialize(Task task) {
        Hibernate.initialize(task.getPredecessorIds());
    }

    /**
     * 前置任務必須是同專案的有效任務、不能是自己，也不能成環：
     * 從新的前置任務沿著「前置的前置」一路往前追，追到自己就表示成環。
     */
    private void requirePredecessors(Long taskId, Long projectId, Set<Long> predecessorIds) {
        if (predecessorIds.isEmpty()) {
            return;
        }
        if (taskId != null && predecessorIds.contains(taskId)) {
            throw new BusinessException("前置任務不能是自己");
        }
        Map<Long, Set<Long>> graph = taskDAO.findByProjectIdAndDbMark(projectId, BaseEntity.VALID)
                .stream().collect(Collectors.toMap(Task::getId, Task::getPredecessorIds));
        List<Long> missing = predecessorIds.stream().filter(id -> !graph.containsKey(id))
                .sorted().toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("前置任務不存在或不在同一專案：" + missing);
        }
        // 新任務還沒有人以它為前置，不會成環
        if (taskId == null) {
            return;
        }
        Deque<Long> pending = new ArrayDeque<>(predecessorIds);
        Set<Long> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            Long current = pending.pop();
            if (current.equals(taskId)) {
                throw new BusinessException("前置任務形成循環");
            }
            if (visited.add(current)) {
                pending.addAll(graph.getOrDefault(current, Set.of()));
            }
        }
    }

    private void requireMilestone(Long projectId, Long milestoneId) {
        milestoneDAO.findById(milestoneId)
                .filter(milestone -> milestone.getDbMark() == BaseEntity.VALID
                        && milestone.getProjectId().equals(projectId))
                .orElseThrow(() -> new BusinessException("里程碑不存在或不在同一專案：" + milestoneId));
    }

    private Long nextSortOrder(Long projectId, TaskStatus status) {
        Long max = taskDAO.findMaxSortOrder(projectId, status);
        return max == null ? GAP : max + GAP;
    }

    private Optional<Task> findValid(Long id) {
        return taskDAO.findById(id).filter(task -> task.getDbMark() == BaseEntity.VALID);
    }

    private void recordChanges(Tracked before, Task after) {
        Long id = after.getId();
        taskActivityRecorder.changed(id, "title", before.title(), after.getTitle());
        taskActivityRecorder.changed(id, "status", before.status(), after.getStatus());
        taskActivityRecorder.changed(id, "priority", before.priority(), after.getPriority());
        taskActivityRecorder.changed(id, "assigneeId", before.assigneeId(), after.getAssigneeId());
        taskActivityRecorder.changed(id, "startDate", before.startDate(), after.getStartDate());
        taskActivityRecorder.changed(id, "dueDate", before.dueDate(), after.getDueDate());
        taskActivityRecorder.changed(id, "milestoneId", before.milestoneId(),
                after.getMilestoneId());
        if (after.getAssigneeId() != null && !after.getAssigneeId().equals(before.assigneeId())) {
            taskNotifier.assigned(after);
        }
        if (after.getStatus() != before.status()) {
            taskNotifier.statusChanged(after, before.status());
        }
    }

    private static void requireUnchanged(String field, Object requested, Object current) {
        if (requested != null && !requested.equals(current)) {
            throw new BusinessException(field + " 建立後不可修改");
        }
    }

    private static void requireDateRange(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && startDate.isAfter(dueDate)) {
            throw new BusinessException("開始日不可晚於到期日");
        }
    }

    /** 要記活動紀錄的欄位，在更新前先記下舊值（entity 更新後就會被覆蓋）。 */
    private record Tracked(String title, TaskStatus status, TaskPriority priority,
            String assigneeId, LocalDate startDate, LocalDate dueDate, Long milestoneId) {

        static Tracked of(Task task) {
            return new Tracked(task.getTitle(), task.getStatus(), task.getPriority(),
                    task.getAssigneeId(), task.getStartDate(), task.getDueDate(),
                    task.getMilestoneId());
        }
    }
}
