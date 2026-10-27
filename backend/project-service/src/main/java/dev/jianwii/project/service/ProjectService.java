package dev.jianwii.project.service;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.project.ProjectQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.project.Project;
import dev.jianwii.entity.project.ProjectMember;
import dev.jianwii.entity.project.ProjectRole;
import dev.jianwii.entity.project.ProjectStatus;
import dev.jianwii.entity.project.TaskStatus;
import dev.jianwii.project.dao.MeetingDAO;
import dev.jianwii.project.dao.ProjectDAO;
import dev.jianwii.project.dao.ProjectMemberDAO;
import dev.jianwii.project.dao.TaskCount;
import dev.jianwii.project.dao.TaskDAO;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 專案 CRUD；建立者自動成為經理，查詢結果帶任務統計。 */
@Service
public class ProjectService extends BaseService<Project, ProjectQueryParameter> {
    /** basic-data 的編號規則代碼，規則由 basic-data 的 SerialNumberSetup 建立。 */
    private static final String SERIAL_KEY = "PROJECT";

    private final ProjectMemberDAO projectMemberDAO;
    // 用 DAO 而非 TaskService，避免兩個 Service 互相注入
    private final TaskDAO taskDAO;
    private final MeetingDAO meetingDAO;
    private final SerialNumberApi serialNumberApi;

    public ProjectService(
            ProjectDAO projectDAO,
            ProjectMemberDAO projectMemberDAO,
            TaskDAO taskDAO,
            MeetingDAO meetingDAO,
            SerialNumberApi serialNumberApi) {
        super(projectDAO);
        this.projectMemberDAO = projectMemberDAO;
        this.taskDAO = taskDAO;
        this.meetingDAO = meetingDAO;
        this.serialNumberApi = serialNumberApi;
    }

    /** 查詢後一次補上這一頁所有專案的任務統計，不會每個專案各查一次。 */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<Project> queryPage(ProjectQueryParameter parameter) {
        PageResponse<Project> page = super.queryPage(parameter);
        List<Project> projects = page.getItems();
        if (!projects.isEmpty()) {
            Map<Long, TaskCount> counts = taskDAO
                    .projectCounts(projects.stream().map(Project::getId).toList())
                    .stream()
                    .collect(Collectors.toMap(TaskCount::getGroupId, Function.identity()));
            for (Project project : projects) {
                TaskCount count = counts.get(project.getId());
                project.setTaskCount(count == null ? 0L : count.getTotal());
                project.setDoneTaskCount(count == null ? 0L : count.getDone());
                project.setOverdueTaskCount(count == null ? 0L : count.getOverdue());
            }
        }
        return page;
    }

    /** 新增後把建立者加為經理；沒有操作者（系統建立）時不加。 */
    @Override
    public Project create(Project request) {
        Project saved = super.create(request);
        String operator = Operator.current();
        if (!Operator.SYSTEM.equals(operator)) {
            ProjectMember manager = new ProjectMember();
            manager.setProjectId(saved.getId());
            manager.setUserId(operator);
            manager.setRole(ProjectRole.MANAGER);
            manager.setDbMark(BaseEntity.VALID);
            projectMemberDAO.save(manager);
        }
        return saved;
    }

    @Override
    protected void beforeCreate(Project request) {
        if (request.getStatus() == null) {
            request.setStatus(ProjectStatus.PLANNING);
        }
        if (request.getStatus().isClosed()) {
            throw new BusinessException("新專案不能是結案或取消");
        }
        requireDateRange(request.getStartDate(), request.getEndDate());
        request.setWipLimits(request.getWipLimits() == null ? new HashMap<>() : new HashMap<>(request.getWipLimits()));
        requireWipLimits(request.getWipLimits());
        // 號碼取走就不會退回，所以檢查都通過才取號，減少跳號
        request.setCode(serialNumberApi.next(SERIAL_KEY));
    }

    @Override
    protected void beforeUpdate(Project entity, Project request) {
        // 系統編號不可修改
        request.setCode(null);
        ProjectStatus status = request.getStatus();
        boolean statusChanged = status != null && status != entity.getStatus();
        if (statusChanged && !entity.getStatus().next().contains(status)) {
            throw new BusinessException("專案狀態不能從 " + entity.getStatus() + " 改為 " + status);
        }
        if (entity.getStatus().isClosed() && !statusChanged) {
            throw new BusinessException("專案已結案或取消，請先重新開啟");
        }
        requireDateRange(
                request.getStartDate() != null ? request.getStartDate() : entity.getStartDate(),
                request.getEndDate() != null ? request.getEndDate() : entity.getEndDate());
        if (request.getWipLimits() != null) {
            requireWipLimits(request.getWipLimits());
            request.setWipLimits(new HashMap<>(request.getWipLimits()));
            // 只改 WIP 上限時主表不會 UPDATE，強制更新修改者與時間
            touch(entity);
        }
    }

    /** 回傳前載入 WIP 上限（LAZY 的中間表）。 */
    @Override
    protected void initialize(Project project) {
        Hibernate.initialize(project.getWipLimits());
    }

    @Override
    protected void beforeDelete(Project entity) {
        if (meetingDAO.existsByProjectIdAndDbMark(entity.getId(), BaseEntity.VALID)) {
            throw new ConflictException("專案仍有會議紀錄，請改為結案或取消");
        }
        if (taskDAO.existsByProjectIdAndDbMark(entity.getId(), BaseEntity.VALID)) {
            throw new ConflictException("專案仍有任務，請先刪除任務，或把專案改為取消");
        }
    }

    /** 成員在另一張表：EXISTS 子查詢，同一個專案不會因多筆成員重複出現。 */
    @Override
    protected Specification<Project> toSpecification(ProjectQueryParameter parameter) {
        Specification<Project> specification = super.toSpecification(parameter);
        String memberUserId = parameter.getMemberUserId();
        if (memberUserId == null) {
            return specification;
        }
        ProjectRole memberRole = parameter.getMemberRole();
        return specification.and((root, query, builder) -> {
            Subquery<Long> members = query.subquery(Long.class);
            Root<ProjectMember> member = members.from(ProjectMember.class);
            List<Predicate> predicates = new ArrayList<>(List.of(
                    builder.equal(member.get("projectId"), root.get("id")),
                    builder.equal(member.get("userId"), memberUserId),
                    builder.equal(member.get("dbMark"), BaseEntity.VALID)));
            if (memberRole != null) {
                predicates.add(builder.equal(member.get("role"), memberRole));
            }
            members.select(member.get("id")).where(predicates.toArray(Predicate[]::new));
            return builder.exists(members);
        });
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "code", "name", "startDate", "endDate");
    }

    /** 只有進行中、審核中可以設上限，值為 1–99。 */
    private static void requireWipLimits(Map<TaskStatus, Integer> limits) {
        for (Map.Entry<TaskStatus, Integer> limit : limits.entrySet()) {
            if (limit.getKey() != TaskStatus.IN_PROGRESS && limit.getKey() != TaskStatus.REVIEW) {
                throw new BusinessException("只有進行中、審核中可以設 WIP 上限：" + limit.getKey());
            }
            if (limit.getValue() == null || limit.getValue() < 1 || limit.getValue() > 99) {
                throw new BusinessException("WIP 上限必須是 1–99：" + limit.getKey());
            }
        }
    }

    private static void requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("開始日不可晚於結束日");
        }
    }
}
