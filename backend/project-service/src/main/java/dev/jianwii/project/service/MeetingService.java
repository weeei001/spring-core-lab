package dev.jianwii.project.service;

import dev.jianwii.api.project.dto.MeetingQueryParameter;
import dev.jianwii.api.project.dto.MeetingTaskCreateRequest;
import dev.jianwii.api.project.dto.MeetingTaskView;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.Meeting;
import dev.jianwii.entity.project.Task;
import dev.jianwii.project.dao.MeetingDAO;
import dev.jianwii.project.dao.ProjectDAO;
import dev.jianwii.project.dao.TaskDAO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingService extends BaseService<Meeting, MeetingQueryParameter> {
    private final ProjectDAO projectDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final TaskService taskService;

    public MeetingService(
            MeetingDAO meetingDAO,
            ProjectDAO projectDAO,
            TaskDAO taskDAO,
            ProjectRules projectRules,
            TaskService taskService) {
        super(meetingDAO);
        this.projectDAO = projectDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.taskService = taskService;
    }

    @Transactional(readOnly = true)
    public Meeting get(Long id) {
        Meeting meeting = find(id);
        requireProject(meeting.getProjectId());
        initialize(meeting);
        return meeting;
    }

    @Transactional(readOnly = true)
    public List<MeetingTaskView> tasks(Long id) {
        Meeting meeting = get(id);
        Map<Long, Task> tasks = taskDAO.findAllById(meeting.getTaskIds()).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        return meeting.getTaskIds().stream()
                .sorted()
                .map(taskId -> {
                    Task task = tasks.get(taskId);
                    return task == null
                            ? new MeetingTaskView(taskId, true, null, null, null, null, null)
                            : toTaskView(task);
                })
                .toList();
    }

    public Meeting linkTask(Long id, Long taskId) {
        Meeting meeting = get(id);
        projectRules.requireOpen(meeting.getProjectId());
        Task task = taskDAO.findById(taskId).orElseThrow(() -> new NotFoundException("查無任務"));
        if (!task.getProjectId().equals(meeting.getProjectId())) {
            throw new BusinessException("任務不屬於指定專案");
        }
        if (!meeting.getTaskIds().contains(taskId) && meeting.getTaskIds().size() >= 100) {
            throw new BusinessException("會議最多連結 100 個任務");
        }
        if (meeting.getTaskIds().add(taskId)) {
            touch(meeting);
            dao.saveAndFlush(meeting);
        }
        return meeting;
    }

    public Meeting unlinkTask(Long id, Long taskId) {
        Meeting meeting = get(id);
        projectRules.requireOpen(meeting.getProjectId());
        if (meeting.getTaskIds().remove(taskId)) {
            touch(meeting);
            dao.saveAndFlush(meeting);
        }
        return meeting;
    }

    public MeetingTaskView createTask(Long id, MeetingTaskCreateRequest request) {
        Meeting meeting = get(id);
        projectRules.requireOpen(meeting.getProjectId());
        if (meeting.getTaskIds().size() >= 100) {
            throw new BusinessException("會議最多連結 100 個任務");
        }
        Task task = new Task();
        task.setProjectId(meeting.getProjectId());
        task.setTitle(request.title());
        task.setAssigneeId(request.assigneeId());
        task.setDueDate(request.dueDate());
        Task saved = taskService.create(task);
        meeting.getTaskIds().add(saved.getId());
        touch(meeting);
        dao.saveAndFlush(meeting);
        return toTaskView(saved);
    }

    private MeetingTaskView toTaskView(Task task) {
        return new MeetingTaskView(
                task.getId(),
                false,
                task.getCode(),
                task.getTitle(),
                task.getAssigneeId(),
                task.getDueDate(),
                task.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Meeting> queryPage(MeetingQueryParameter parameter) {
        requireProject(parameter.getProjectId());
        if (parameter.getTaskId() != null) {
            var task = taskDAO.findById(parameter.getTaskId()).orElseThrow(() -> new NotFoundException("查無任務"));
            if (!task.getProjectId().equals(parameter.getProjectId())) {
                throw new BusinessException("任務不屬於指定專案");
            }
        }
        if (parameter.getSort().isEmpty()) {
            QueryParameter.Order order = new QueryParameter.Order();
            order.setField("heldAt");
            order.setDirection("desc");
            parameter.setSort(List.of(order));
        }
        return super.queryPage(parameter);
    }

    @Override
    protected Specification<Meeting> toSpecification(MeetingQueryParameter parameter) {
        Specification<Meeting> specification = super.toSpecification(parameter);
        return parameter.getTaskId() == null
                ? specification
                : specification.and(
                        (root, query, builder) -> builder.isMember(parameter.getTaskId(), root.get("taskIds")));
    }

    @Override
    protected void beforeCreate(Meeting request) {
        requireProject(request.getProjectId());
        projectRules.requireOpen(request.getProjectId());
        if (request.getTaskIds() != null) {
            throw new BusinessException("taskIds 不能由紀錄寫入，須使用關聯 API");
        }
        request.setTaskIds(Set.of());
    }

    @Override
    protected void beforeUpdate(Meeting entity, Meeting request) {
        requireProject(entity.getProjectId());
        projectRules.requireOpen(entity.getProjectId());
        if (request.getProjectId() != null && !request.getProjectId().equals(entity.getProjectId())) {
            throw new BusinessException("會議建立後不可換專案");
        }
        if (request.getTaskIds() != null) {
            throw new BusinessException("taskIds 不能由紀錄寫入，須使用關聯 API");
        }
        request.setProjectId(null);
    }

    @Override
    protected void initialize(Meeting meeting) {
        Hibernate.initialize(meeting.getTaskIds());
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("heldAt", "createTime", "transTime", "id");
    }

    private void requireProject(Long projectId) {
        if (projectId == null || projectDAO.findById(projectId).isEmpty()) {
            throw new NotFoundException("查無專案");
        }
    }
}
