package dev.jianwii.project.service;

import dev.jianwii.api.project.ProjectLabelQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.ProjectLabel;
import dev.jianwii.entity.project.Task;
import dev.jianwii.project.dao.ProjectLabelDAO;
import dev.jianwii.project.dao.TaskCount;
import dev.jianwii.project.dao.TaskDAO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 專案標籤 CRUD；同專案名稱不可重複，刪除時從任務上移除。 */
@Service
public class ProjectLabelService extends BaseService<ProjectLabel, ProjectLabelQueryParameter> {
    private final ProjectLabelDAO projectLabelDAO;
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;

    public ProjectLabelService(ProjectLabelDAO projectLabelDAO, TaskDAO taskDAO, ProjectRules projectRules) {
        super(projectLabelDAO);
        this.projectLabelDAO = projectLabelDAO;
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
    }

    /** 查詢後一次補上這一頁所有標籤的任務數。 */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectLabel> queryPage(ProjectLabelQueryParameter parameter) {
        PageResponse<ProjectLabel> page = super.queryPage(parameter);
        List<ProjectLabel> labels = page.getItems();
        if (!labels.isEmpty()) {
            Map<Long, Long> counts = taskDAO
                    .labelCounts(labels.stream().map(ProjectLabel::getId).toList())
                    .stream()
                    .collect(Collectors.toMap(TaskCount::getGroupId, TaskCount::getTotal));
            for (ProjectLabel label : labels) {
                label.setTaskCount(counts.getOrDefault(label.getId(), 0L));
            }
        }
        return page;
    }

    @Override
    protected void beforeCreate(ProjectLabel request) {
        projectRules.requireOpen(request.getProjectId());
        request.setName(request.getName().strip());
        request.setColor(request.getColor().toUpperCase(Locale.ROOT));
        requireUniqueName(request.getProjectId(), null, request.getName());
    }

    @Override
    protected void beforeUpdate(ProjectLabel entity, ProjectLabel request) {
        request.setProjectId(null);
        projectRules.requireOpen(entity.getProjectId());
        if (request.getName() != null) {
            request.setName(request.getName().strip());
            requireUniqueName(entity.getProjectId(), entity.getId(), request.getName());
        }
        if (request.getColor() != null) {
            request.setColor(request.getColor().toUpperCase(Locale.ROOT));
        }
    }

    /** 掛了這個標籤的任務一併移除它（含已取消的任務，已刪除的不動）。 */
    @Override
    protected void beforeDelete(ProjectLabel entity) {
        projectRules.requireOpen(entity.getProjectId());
        List<Task> tagged = taskDAO.findByLabelIdsContaining(entity.getId());
        for (Task task : tagged) {
            task.getLabelIds().remove(entity.getId());
            // 只改中間表時主表不會 UPDATE，強制更新修改者與時間
            task.setTransTime(LocalDateTime.now());
        }
        taskDAO.saveAll(tagged);
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "name");
    }

    private void requireUniqueName(Long projectId, Long selfId, String name) {
        boolean duplicated = projectLabelDAO.findByProjectId(projectId).stream()
                .anyMatch(label ->
                        !label.getId().equals(selfId) && label.getName().equalsIgnoreCase(name));
        if (duplicated) {
            throw new ConflictException("此專案已有同名標籤：" + name);
        }
    }
}
