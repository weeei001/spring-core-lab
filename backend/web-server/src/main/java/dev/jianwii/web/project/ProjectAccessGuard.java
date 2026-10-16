package dev.jianwii.web.project;

import dev.jianwii.api.project.MilestoneApi;
import dev.jianwii.api.project.MilestoneQueryParameter;
import dev.jianwii.api.project.ProjectLabelApi;
import dev.jianwii.api.project.ProjectLabelQueryParameter;
import dev.jianwii.api.project.ProjectMemberApi;
import dev.jianwii.api.project.ProjectMemberQueryParameter;
import dev.jianwii.api.project.ProjectQueryParameter;
import dev.jianwii.api.project.SprintApi;
import dev.jianwii.api.project.SprintQueryParameter;
import dev.jianwii.api.project.TaskApi;
import dev.jianwii.api.project.TaskAttachmentApi;
import dev.jianwii.api.project.TaskAttachmentQueryParameter;
import dev.jianwii.api.project.TaskCommentApi;
import dev.jianwii.api.project.TaskCommentQueryParameter;
import dev.jianwii.api.project.TaskQueryParameter;
import dev.jianwii.api.project.WorkLogApi;
import dev.jianwii.api.project.WorkLogQueryParameter;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.web.Operator;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.project.ProjectMember;
import dev.jianwii.entity.project.ProjectRole;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskAttachment;
import dev.jianwii.entity.project.TaskComment;
import dev.jianwii.entity.project.WorkLog;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 專案的資料範圍：一般使用者只能存取自己是成員的專案，有 project:admin 的人不受限制。
 * 功能權限由 Controller 的 {@code @PreAuthorize} 先擋，這裡只管「是不是這個專案的人」；
 * project-service 信任 web-server，不再重做這些檢查。
 *
 * <ul>
 * <li>查詢：scopeXxx 依參數檢查成員資格，或把條件限縮到登入者自己的資料。</li>
 * <li>寫入：requireMember／requireManager，id 先用 projectOfXxx 換成所屬專案。</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class ProjectAccessGuard {
    private final ProjectMemberApi projectMemberApi;
    private final MilestoneApi milestoneApi;
    private final ProjectLabelApi projectLabelApi;
    private final SprintApi sprintApi;
    private final TaskApi taskApi;
    private final TaskCommentApi taskCommentApi;
    private final TaskAttachmentApi taskAttachmentApi;
    private final WorkLogApi workLogApi;

    // ---- 查詢 ----

    /** 一般使用者只查得到自己是成員的專案。 */
    public void scopeProjects(ProjectQueryParameter parameter) {
        if (!isAdmin()) {
            parameter.setMemberUserId(currentUserId());
        }
    }

    /** 給 projectId 時要是該專案成員；沒給時只查得到自己的成員資料（我參與哪些專案）。 */
    public void scopeMembers(ProjectMemberQueryParameter parameter) {
        if (isAdmin()) {
            return;
        }
        if (parameter.getProjectId() != null) {
            requireMember(parameter.getProjectId());
        } else {
            parameter.setUserId(currentUserId());
        }
    }

    /** 必須指定專案，且都是自己參與的專案。 */
    public void scopeMilestones(MilestoneQueryParameter parameter) {
        requireMemberOfProjects(parameter.getProjectId(), parameter.getProjectIds());
    }

    /** 必須指定專案，且都是自己參與的專案。 */
    public void scopeLabels(ProjectLabelQueryParameter parameter) {
        requireMemberOfProjects(parameter.getProjectId(), parameter.getProjectIds());
    }

    /** 必須指定專案，且都是自己參與的專案。 */
    public void scopeSprints(SprintQueryParameter parameter) {
        requireMemberOfProjects(parameter.getProjectId(), parameter.getProjectIds());
    }

    /**
     * 給 projectId 時要是該專案成員；只給 ids 時（任務抽屜、工時表）這些任務的專案都要是自己參與的；
     * 都沒給時只查得到指派給自己的任務（我的任務）。
     */
    public void scopeTasks(TaskQueryParameter parameter) {
        if (isAdmin()) {
            return;
        }
        if (parameter.getProjectId() != null) {
            requireMember(parameter.getProjectId());
        } else if (parameter.getIds() != null && !parameter.getIds().isEmpty()) {
            TaskQueryParameter byIds = new TaskQueryParameter();
            byIds.setIds(parameter.getIds());
            byIds.setSize(100);
            requireMemberOfAll(taskApi.query(byIds).stream().map(Task::getProjectId)
                    .collect(Collectors.toSet()));
        } else {
            parameter.setAssigneeId(currentUserId());
        }
    }

    /** 必須指定任務，且是該任務所屬專案的成員。 */
    public void scopeComments(TaskCommentQueryParameter parameter) {
        if (isAdmin()) {
            return;
        }
        if (parameter.getTaskId() == null) {
            throw new BusinessException("請指定 taskId");
        }
        requireMember(projectOfTask(parameter.getTaskId()));
    }

    /** 必須指定任務，且是該任務所屬專案的成員。 */
    public void scopeAttachments(TaskAttachmentQueryParameter parameter) {
        if (isAdmin()) {
            return;
        }
        if (parameter.getTaskId() == null) {
            throw new BusinessException("請指定 taskId");
        }
        requireMember(projectOfTask(parameter.getTaskId()));
    }

    /** 給 projectId 或 taskId 時要是該專案成員；都沒給時只查得到自己的工時。 */
    public void scopeWorkLogs(WorkLogQueryParameter parameter) {
        if (isAdmin()) {
            return;
        }
        if (parameter.getProjectId() != null) {
            requireMember(parameter.getProjectId());
        } else if (parameter.getTaskId() != null) {
            requireMember(projectOfTask(parameter.getTaskId()));
        } else {
            parameter.setUserId(currentUserId());
        }
    }

    // ---- 寫入 ----

    public void requireMember(Long projectId) {
        if (!isAdmin() && role(projectId).isEmpty()) {
            throw new ForbiddenException("不是此專案的成員");
        }
    }

    public void requireManager(Long projectId) {
        if (!isAdmin() && role(projectId).filter(ProjectRole.MANAGER::equals).isEmpty()) {
            throw new ForbiddenException("只有專案經理可以執行此操作");
        }
    }

    // ---- 由 id 找所屬專案；找不到回 404 ----

    public Long projectOfMember(Long memberId) {
        ProjectMemberQueryParameter parameter = new ProjectMemberQueryParameter();
        parameter.setIds(List.of(memberId));
        return first(projectMemberApi.query(parameter), memberId).getProjectId();
    }

    public Long projectOfMilestone(Long milestoneId) {
        MilestoneQueryParameter parameter = new MilestoneQueryParameter();
        parameter.setIds(List.of(milestoneId));
        return first(milestoneApi.query(parameter), milestoneId).getProjectId();
    }

    public Long projectOfSprint(Long sprintId) {
        SprintQueryParameter parameter = new SprintQueryParameter();
        parameter.setIds(List.of(sprintId));
        return first(sprintApi.query(parameter), sprintId).getProjectId();
    }

    public Long projectOfLabel(Long labelId) {
        ProjectLabelQueryParameter parameter = new ProjectLabelQueryParameter();
        parameter.setIds(List.of(labelId));
        return first(projectLabelApi.query(parameter), labelId).getProjectId();
    }

    public Long projectOfTask(Long taskId) {
        TaskQueryParameter parameter = new TaskQueryParameter();
        parameter.setIds(List.of(taskId));
        return first(taskApi.query(parameter), taskId).getProjectId();
    }

    public Long projectOfComment(Long commentId) {
        TaskCommentQueryParameter parameter = new TaskCommentQueryParameter();
        parameter.setIds(List.of(commentId));
        TaskComment comment = first(taskCommentApi.query(parameter), commentId);
        return projectOfTask(comment.getTaskId());
    }

    public Long projectOfAttachment(Long attachmentId) {
        TaskAttachmentQueryParameter parameter = new TaskAttachmentQueryParameter();
        parameter.setIds(List.of(attachmentId));
        TaskAttachment attachment = first(taskAttachmentApi.query(parameter), attachmentId);
        return projectOfTask(attachment.getTaskId());
    }

    public Long projectOfWorkLog(Long workLogId) {
        WorkLogQueryParameter parameter = new WorkLogQueryParameter();
        parameter.setIds(List.of(workLogId));
        WorkLog workLog = first(workLogApi.query(parameter), workLogId);
        return workLog.getProjectId();
    }

    /** 查詢必須指定 projectId 或 projectIds，且都是登入者參與的專案。 */
    private void requireMemberOfProjects(Long projectId, List<Long> otherProjectIds) {
        if (isAdmin()) {
            return;
        }
        Set<Long> projectIds = new HashSet<>();
        if (projectId != null) {
            projectIds.add(projectId);
        }
        if (otherProjectIds != null) {
            projectIds.addAll(otherProjectIds);
        }
        if (projectIds.isEmpty()) {
            throw new BusinessException("請指定 projectId 或 projectIds");
        }
        requireMemberOfAll(projectIds);
    }

    /** 這些專案都必須是登入者參與的。 */
    private void requireMemberOfAll(Set<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return;
        }
        // 超過成員查詢的上限（例如 projectId 加上 100 個 projectIds）
        if (projectIds.size() > 100) {
            throw new BusinessException("一次最多查詢 100 個專案");
        }
        ProjectMemberQueryParameter mine = new ProjectMemberQueryParameter();
        mine.setUserId(currentUserId());
        mine.setProjectIds(List.copyOf(projectIds));
        mine.setSize(100);
        Set<Long> joined = new HashSet<>(
                projectMemberApi.query(mine).stream().map(ProjectMember::getProjectId).toList());
        if (!joined.containsAll(projectIds)) {
            throw new ForbiddenException("不是此專案的成員");
        }
    }

    private Optional<ProjectRole> role(Long projectId) {
        ProjectMemberQueryParameter parameter = new ProjectMemberQueryParameter();
        parameter.setProjectId(projectId);
        parameter.setUserId(currentUserId());
        return projectMemberApi.query(parameter).stream().findFirst().map(ProjectMember::getRole);
    }

    private static <T> T first(List<T> items, Long id) {
        return items.stream().findFirst()
                .orElseThrow(() -> new NotFoundException("查無資料，id：" + id));
    }

    /** 經 OperatorHeaderFilter 改寫後的 X-Operator，即登入者的 user id。 */
    private static String currentUserId() {
        return Operator.current();
    }

    /** PermissionAuthorityFilter 已把登入者的功能權限放進 authorities。 */
    private static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Permission.PROJECT_ADMIN.getCode()::equals);
    }
}
