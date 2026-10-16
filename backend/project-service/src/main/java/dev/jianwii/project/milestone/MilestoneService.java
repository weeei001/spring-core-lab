package dev.jianwii.project.milestone;

import dev.jianwii.api.project.MilestoneQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.ConflictException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.project.Milestone;
import dev.jianwii.project.project.ProjectRules;
import dev.jianwii.project.task.TaskCount;
import dev.jianwii.project.task.TaskDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 里程碑 CRUD；查詢結果帶任務統計，仍有任務時不可刪除。 */
@Service
public class MilestoneService extends BaseService<Milestone, MilestoneQueryParameter> {
    private final TaskDAO taskDAO;
    private final ProjectRules projectRules;

    public MilestoneService(MilestoneDAO milestoneDAO, TaskDAO taskDAO,
            ProjectRules projectRules) {
        super(milestoneDAO);
        this.taskDAO = taskDAO;
        this.projectRules = projectRules;
    }

    /** 查詢後一次補上這一頁所有里程碑的任務統計。 */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<Milestone> queryPage(MilestoneQueryParameter parameter) {
        PageResponse<Milestone> page = super.queryPage(parameter);
        List<Milestone> milestones = page.getItems();
        if (!milestones.isEmpty()) {
            Map<Long, TaskCount> counts = taskDAO.milestoneCounts(
                    milestones.stream().map(Milestone::getId).toList()).stream()
                    .collect(Collectors.toMap(TaskCount::getGroupId, Function.identity()));
            for (Milestone milestone : milestones) {
                TaskCount count = counts.get(milestone.getId());
                milestone.setTaskCount(count == null ? 0L : count.getTotal());
                milestone.setDoneTaskCount(count == null ? 0L : count.getDone());
            }
        }
        return page;
    }

    @Override
    protected void beforeCreate(Milestone request) {
        projectRules.requireOpen(request.getProjectId());
        if (request.getAchieved() == null) {
            request.setAchieved(false);
        }
    }

    @Override
    protected void beforeUpdate(Milestone entity, Milestone request) {
        request.setProjectId(null);
        projectRules.requireOpen(entity.getProjectId());
    }

    @Override
    protected void beforeDelete(Milestone entity) {
        projectRules.requireOpen(entity.getProjectId());
        if (taskDAO.existsByMilestoneIdAndDbMark(entity.getId(), BaseEntity.VALID)) {
            throw new ConflictException("仍有任務掛在此里程碑：" + entity.getName());
        }
    }

    @Override
    protected Set<String> sortableFields() {
        return Set.of("id", "createTime", "transTime", "dueDate", "name");
    }
}
