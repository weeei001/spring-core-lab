package dev.jianwii.project.service;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.entity.project.SprintBurndown;
import dev.jianwii.entity.project.SprintStatus;
import dev.jianwii.project.dao.SprintBurndownDAO;
import dev.jianwii.project.dao.SprintCount;
import dev.jianwii.project.dao.SprintDAO;
import dev.jianwii.project.dao.TaskDAO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 寫入燃盡圖的每日紀錄（{@link SprintBurndown}）：任務、Sprint 的 Service 在異動後同一個交易內呼叫，
 * 以當下的合計更新今天這一筆。只記進行中的 Sprint。
 */
@Component
@AllArgsConstructor
public class SprintBurndownRecorder {
    private final SprintDAO sprintDAO;
    private final SprintBurndownDAO sprintBurndownDAO;
    private final TaskDAO taskDAO;

    /** sprintId 為 null、或不是進行中的 Sprint 時不做事。 */
    public void refresh(Long sprintId) {
        if (sprintId == null || sprintDAO.findById(sprintId)
                .filter(sprint -> sprint.getDbMark() == BaseEntity.VALID
                        && sprint.getStatus() == SprintStatus.ACTIVE)
                .isEmpty()) {
            return;
        }
        List<SprintCount> counts = taskDAO.sprintCounts(List.of(sprintId));
        SprintCount count = counts.isEmpty() ? null : counts.getFirst();
        LocalDate today = LocalDate.now();
        SprintBurndown row = sprintBurndownDAO.findBySprintIdAndDay(sprintId, today)
                .orElseGet(() -> {
                    SprintBurndown created = new SprintBurndown();
                    created.setSprintId(sprintId);
                    created.setDay(today);
                    created.setDbMark(BaseEntity.VALID);
                    return created;
                });
        row.setTaskCount(count == null ? 0L : count.getTotal());
        row.setDoneTaskCount(count == null ? 0L : count.getDone());
        row.setTotalPoints(count == null ? 0L : count.getPoints());
        row.setDonePoints(count == null ? 0L : count.getDonePoints());
        sprintBurndownDAO.save(row);
    }
}
