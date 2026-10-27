package dev.jianwii.project.service;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.project.dto.TaskQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.ProjectLabel;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskPriority;
import dev.jianwii.entity.project.TaskStatus;
import dev.jianwii.entity.project.TaskType;
import dev.jianwii.project.dao.ProjectLabelDAO;
import dev.jianwii.project.dao.TaskCount;
import dev.jianwii.project.dao.TaskDAO;
import dev.jianwii.project.dao.TaskHours;
import dev.jianwii.project.dao.WorkLogDAO;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任務 CRUD 與看板排序；子任務、前置任務、指派人、標籤的檢查，以及欄位變更的活動紀錄。
 */
@Service
public class TaskService extends BaseService<Task, TaskQueryParameter> {
    /** basic-data 的編號規則代碼，規則由 basic-data 的 SerialNumberSetup 建立。 */
    private static final String SERIAL_KEY = "TASK";
    /** 看板排序值的間隔：插到兩張卡片之間時取中間值，間隔用完才整欄重新編號，平常只改被拖的那一筆。 */
    private static final long GAP = 1024;
    /** 可用 clearFields 清空的欄位。 */
    private static final Set<String> CLEARABLE =
            Set.of("assigneeId", "startDate", "dueDate", "estimateHours", "description");

    private final TaskDAO taskDAO;
    private final ProjectLabelDAO projectLabelDAO;
    private final WorkLogDAO workLogDAO;
    private final ProjectRules projectRules;
    private final TaskActivityRecorder taskActivityRecorder;
    private final TaskNotifier taskNotifier;
    private final SerialNumberApi serialNumberApi;

    public TaskService(
            TaskDAO taskDAO,
            ProjectLabelDAO projectLabelDAO,
            WorkLogDAO workLogDAO,
            ProjectRules projectRules,
            TaskActivityRecorder taskActivityRecorder,
            TaskNotifier taskNotifier,
            SerialNumberApi serialNumberApi) {
        super(taskDAO);
        this.taskDAO = taskDAO;
        this.projectLabelDAO = projectLabelDAO;
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

    /** 登入者關注任務；已關注時不變。 */
    public Task watch(Long id) {
        Task task = find(id);
        projectRules.requireOpen(task.getProjectId());
        String operator = Operator.current();
        projectRules.requireMember(task.getProjectId(), operator);
        if (task.getWatcherIds().add(operator)) {
            touch(task);
        }
        Task saved = taskDAO.saveAndFlush(task);
        initialize(saved);
        return saved;
    }

    /** 登入者取消關注；指派人仍會收到通知。 */
    public Task unwatch(Long id) {
        Task task = find(id);
        projectRules.requireOpen(task.getProjectId());
        if (task.getWatcherIds().remove(Operator.current())) {
            touch(task);
        }
        Task saved = taskDAO.saveAndFlush(task);
        initialize(saved);
        return saved;
    }

    /** 把 userId 加入關注者（不檢查成員資格，呼叫端負責）；系統操作不加。 */
    public static void addWatcher(Task task, String userId) {
        if (userId != null && !userId.equals(Operator.SYSTEM)) {
            task.getWatcherIds().add(userId);
        }
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
     * 看板拖曳：改成 status，排在同欄位 previousId 之後（null 表示排第一）。 平常只改這一筆的排序值；前後兩張卡片之間沒有空隙時，整欄重新編號。
     */
    public Task move(Long id, TaskStatus status, Long previousId) {
        Task task = find(id);
        projectRules.requireOpen(task.getProjectId());
        Tracked before = Tracked.of(task);
        List<Task> column =
                new ArrayList<>(taskDAO.findByProjectIdAndStatusOrderBySortOrderAscIdAsc(task.getProjectId(), status));
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
            Task parent = taskDAO.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException("父任務不存在：" + request.getParentId()));
            if (!parent.getProjectId().equals(projectId)) {
                throw new BusinessException("父任務必須在同一個專案");
            }
            if (parent.getParentId() != null) {
                throw new BusinessException("子任務底下不能再有子任務");
            }
        }
        if (request.getAssigneeId() != null) {
            projectRules.requireMember(projectId, request.getAssigneeId());
        }
        requireDateRange(request.getStartDate(), request.getDueDate());
        if (request.getPredecessorIds() == null) {
            request.setPredecessorIds(new HashSet<>());
        }
        requirePredecessors(null, projectId, request.getPredecessorIds());
        if (request.getLabelIds() == null) {
            request.setLabelIds(new HashSet<>());
        }
        requireLabels(projectId, request.getLabelIds());
        if (request.getType() == null) {
            request.setType(TaskType.FEATURE);
        }
        request.setWatcherIds(new HashSet<>());
        addWatcher(request, Operator.current());
        addWatcher(request, request.getAssigneeId());
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
        request.setWatcherIds(null);

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

        if (request.getAssigneeId() != null) {
            projectRules.requireMember(projectId, request.getAssigneeId());
            addWatcher(entity, request.getAssigneeId());
        }
        LocalDate startDate = clearFields.contains("startDate")
                ? null
                : request.getStartDate() != null ? request.getStartDate() : entity.getStartDate();
        LocalDate dueDate = clearFields.contains("dueDate")
                ? null
                : request.getDueDate() != null ? request.getDueDate() : entity.getDueDate();
        requireDateRange(startDate, dueDate);
        if (request.getPredecessorIds() != null) {
            requirePredecessors(entity.getId(), projectId, request.getPredecessorIds());
            // 只改前置任務時主表不會 UPDATE，強制更新修改者與時間
            touch(entity);
        }
        if (request.getLabelIds() != null) {
            requireLabels(projectId, request.getLabelIds());
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
        if (taskDAO.existsByParentId(entity.getId())) {
            throw new ConflictException("請先刪除子任務：" + entity.getCode());
        }
        List<Task> dependents = taskDAO.findByPredecessorIdsContaining(entity.getId());
        for (Task dependent : dependents) {
            dependent.getPredecessorIds().remove(entity.getId());
            touch(dependent);
        }
        taskDAO.saveAll(dependents);
    }

    /**
     * 條件清單表達不了的條件在這裡補上：topLevel 是 parentId IS NULL； labelIds 在中間表，用 EXISTS 子查詢，同一個任務不會因多個標籤重複出現。
     */
    @Override
    protected Specification<Task> toSpecification(TaskQueryParameter parameter) {
        Specification<Task> specification =
                super.toSpecification(parameter).and(ProjectMemberScope.forUser(parameter.getMemberUserId()));
        Boolean topLevel = parameter.getTopLevel();
        if (topLevel != null) {
            specification = specification.and((root, query, builder) ->
                    topLevel ? builder.isNull(root.get("parentId")) : builder.isNotNull(root.get("parentId")));
        }
        List<Long> labelIds = parameter.getLabelIds();
        if (labelIds != null && !labelIds.isEmpty()) {
            specification = specification.and((root, query, builder) -> {
                Subquery<Long> tagged = query.subquery(Long.class);
                Root<Task> task = tagged.from(Task.class);
                Join<Task, Long> label = task.join("labelIds");
                tagged.select(task.get("id")).where(builder.equal(task.get("id"), root.get("id")), label.in(labelIds));
                return builder.exists(tagged);
            });
        }
        return specification;
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "code", "title", "sortOrder", "startDate", "dueDate");
    }

    /** 回傳前載入前置任務、標籤與關注者（LAZY 的中間表）。 */
    @Override
    protected void initialize(Task task) {
        Hibernate.initialize(task.getPredecessorIds());
        Hibernate.initialize(task.getLabelIds());
        Hibernate.initialize(task.getWatcherIds());
    }

    /**
     * 前置任務必須是同專案的有效任務、不能是自己，也不能成環： 從新的前置任務沿著「前置的前置」一路往前追，追到自己就表示成環。
     */
    private void requirePredecessors(Long taskId, Long projectId, Set<Long> predecessorIds) {
        if (predecessorIds.isEmpty()) {
            return;
        }
        if (taskId != null && predecessorIds.contains(taskId)) {
            throw new BusinessException("前置任務不能是自己");
        }
        Map<Long, Set<Long>> graph = taskDAO.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(Task::getId, Task::getPredecessorIds));
        List<Long> missing = predecessorIds.stream()
                .filter(id -> !graph.containsKey(id))
                .sorted()
                .toList();
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

    /** 標籤必須是同專案的有效標籤。 */
    private void requireLabels(Long projectId, Set<Long> labelIds) {
        if (labelIds.isEmpty()) {
            return;
        }
        Set<Long> valid = projectLabelDAO.findByProjectId(projectId).stream()
                .map(ProjectLabel::getId)
                .collect(Collectors.toSet());
        List<Long> missing =
                labelIds.stream().filter(id -> !valid.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("標籤不存在或不在同一專案：" + missing);
        }
    }

    private Long nextSortOrder(Long projectId, TaskStatus status) {
        Long max = taskDAO.findMaxSortOrder(projectId, status);
        return max == null ? GAP : max + GAP;
    }

    private void recordChanges(Tracked before, Task after) {
        Long id = after.getId();
        taskActivityRecorder.changed(id, "title", before.title(), after.getTitle());
        taskActivityRecorder.changed(id, "type", before.type(), after.getType());
        taskActivityRecorder.changed(id, "labelIds", idsText(before.labelIds()), idsText(after.getLabelIds()));
        taskActivityRecorder.changed(id, "status", before.status(), after.getStatus());
        taskActivityRecorder.changed(id, "priority", before.priority(), after.getPriority());
        taskActivityRecorder.changed(id, "assigneeId", before.assigneeId(), after.getAssigneeId());
        taskActivityRecorder.changed(id, "startDate", before.startDate(), after.getStartDate());
        taskActivityRecorder.changed(id, "dueDate", before.dueDate(), after.getDueDate());
        if (after.getAssigneeId() != null && !after.getAssigneeId().equals(before.assigneeId())) {
            taskNotifier.assigned(after);
        }
        if (after.getStatus() != before.status()) {
            taskNotifier.statusChanged(after, before.status());
        }
    }

    /** 標籤的活動紀錄值：id 由小到大以逗號分隔，沒有標籤為 null。 */
    private static String idsText(Set<Long> ids) {
        return ids == null || ids.isEmpty()
                ? null
                : ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
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
    private record Tracked(
            String title,
            TaskType type,
            Set<Long> labelIds,
            TaskStatus status,
            TaskPriority priority,
            String assigneeId,
            LocalDate startDate,
            LocalDate dueDate) {

        static Tracked of(Task task) {
            // 標籤要複製一份：更新時會換掉 entity 的集合
            return new Tracked(
                    task.getTitle(),
                    task.getType(),
                    Set.copyOf(task.getLabelIds()),
                    task.getStatus(),
                    task.getPriority(),
                    task.getAssigneeId(),
                    task.getStartDate(),
                    task.getDueDate());
        }
    }
}
