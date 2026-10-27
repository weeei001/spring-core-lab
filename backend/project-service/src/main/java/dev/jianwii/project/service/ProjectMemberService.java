package dev.jianwii.project.service;

import dev.jianwii.api.basicdata.api.UserApi;
import dev.jianwii.api.basicdata.dto.UserQueryParameter;
import dev.jianwii.api.project.dto.ProjectMemberQueryParameter;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.ProjectMember;
import dev.jianwii.entity.project.ProjectRole;
import dev.jianwii.entity.project.Task;
import dev.jianwii.project.dao.ProjectMemberDAO;
import dev.jianwii.project.dao.TaskDAO;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 專案成員；每個專案至少保留一位經理。 */
@Service
public class ProjectMemberService extends BaseService<ProjectMember, ProjectMemberQueryParameter> {
    private final ProjectMemberDAO projectMemberDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;
    private final TaskActivityRecorder taskActivityRecorder;
    private final UserApi userApi;

    public ProjectMemberService(
            ProjectMemberDAO projectMemberDAO,
            TaskDAO taskDAO,
            ProjectRules projectRules,
            TaskActivityRecorder taskActivityRecorder,
            UserApi userApi) {
        super(projectMemberDAO);
        this.projectMemberDAO = projectMemberDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
        this.taskActivityRecorder = taskActivityRecorder;
        this.userApi = userApi;
    }

    @Override
    protected void beforeCreate(ProjectMember request) {
        projectRules.requireOpen(request.getProjectId());
        if (projectMemberDAO.existsByProjectIdAndUserId(request.getProjectId(), request.getUserId())) {
            throw new ConflictException("已經是此專案的成員：" + request.getUserId());
        }
        requireUser(request.getUserId());
    }

    @Override
    protected void beforeUpdate(ProjectMember entity, ProjectMember request) {
        // 只能改角色
        request.setProjectId(null);
        request.setUserId(null);
        projectRules.requireOpen(entity.getProjectId());
        if (entity.getRole() == ProjectRole.MANAGER && request.getRole() == ProjectRole.MEMBER) {
            requireAnotherManager(entity);
        }
    }

    /** 移除成員：指派給該成員的未完成任務改為未指派，並記在任務的活動紀錄；也不再關注專案內的任務。 */
    @Override
    protected void beforeDelete(ProjectMember entity) {
        projectRules.requireOpen(entity.getProjectId());
        if (entity.getRole() == ProjectRole.MANAGER) {
            requireAnotherManager(entity);
        }
        List<Task> tasks = taskDAO.findByProjectIdAndAssigneeId(entity.getProjectId(), entity.getUserId()).stream()
                .filter(task -> !task.getStatus().isFinished())
                .toList();
        for (Task task : tasks) {
            taskActivityRecorder.changed(task.getId(), "assigneeId", task.getAssigneeId(), null);
            task.setAssigneeId(null);
        }
        taskDAO.saveAll(tasks);
        List<Task> watched = taskDAO.findByProjectIdAndWatcherIdsContaining(entity.getProjectId(), entity.getUserId());
        watched.forEach(task -> task.getWatcherIds().remove(entity.getUserId()));
        taskDAO.saveAll(watched);
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "role");
    }

    private void requireAnotherManager(ProjectMember manager) {
        if (projectMemberDAO.countByProjectIdAndRole(manager.getProjectId(), ProjectRole.MANAGER) <= 1) {
            throw new BusinessException("專案至少要有一位經理");
        }
    }

    /** userId 必須是 basic-data 裡的有效使用者。 */
    private void requireUser(String userId) {
        UserQueryParameter parameter = new UserQueryParameter();
        parameter.setIds(List.of(userId));
        if (userApi.query(parameter).isEmpty()) {
            throw new BusinessException("使用者不存在：" + userId);
        }
    }
}
