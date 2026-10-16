package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 統計查詢只算有效、未取消的任務；done 為完成數，overdue 為過了到期日仍未完成的數量。
 * dbMark = 0 即 BaseEntity.VALID。
 */
public interface TaskDAO extends BaseDAO<Task, Long> {

    /** 看板一欄（同專案、同狀態）的任務，依排序。 */
    List<Task> findByProjectIdAndStatusAndDbMarkOrderBySortOrderAscIdAsc(Long projectId,
            TaskStatus status, Integer dbMark);

    /** 欄位最後一張的排序值；欄位是空的時回 null。含已刪除的任務，不影響排序。 */
    @Query("select max(t.sortOrder) from Task t where t.projectId = :projectId and t.status = :status")
    Long findMaxSortOrder(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    /** 專案內所有有效任務；檢查前置任務是否成環時用。 */
    List<Task> findByProjectIdAndDbMark(Long projectId, Integer dbMark);

    /** 移除成員時，找出指派給該成員的任務。 */
    List<Task> findByProjectIdAndAssigneeIdAndDbMark(Long projectId, String assigneeId,
            Integer dbMark);

    /** 前置任務含此任務的有效任務；刪除任務時一併移除關聯。 */
    List<Task> findByPredecessorIdsContainingAndDbMark(Long predecessorId, Integer dbMark);

    /** Sprint 內的有效任務；結束、刪除 Sprint 時移動任務用。 */
    List<Task> findBySprintIdAndDbMark(Long sprintId, Integer dbMark);

    /** 移除成員時，找出該成員關注的任務。 */
    List<Task> findByProjectIdAndWatcherIdsContainingAndDbMark(Long projectId, String watcherId,
            Integer dbMark);

    /** 標籤含此標籤的有效任務；刪除標籤時一併移除關聯。 */
    List<Task> findByLabelIdsContainingAndDbMark(Long labelId, Integer dbMark);

    boolean existsByProjectIdAndDbMark(Long projectId, Integer dbMark);

    boolean existsByMilestoneIdAndDbMark(Long milestoneId, Integer dbMark);

    boolean existsByParentIdAndDbMark(Long parentId, Integer dbMark);

    /** 各專案的主任務計數。 */
    @Query("""
            select t.projectId as groupId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   sum(case when t.status <> :done and t.dueDate < :today then 1 else 0 end) as overdue
            from Task t
            where t.projectId in :ids and t.parentId is null and t.status <> :cancelled and t.dbMark = 0
            group by t.projectId""")
    List<TaskCount> countByProject(@Param("ids") Collection<Long> projectIds,
            @Param("done") TaskStatus done, @Param("cancelled") TaskStatus cancelled,
            @Param("today") LocalDate today);

    /** 各里程碑的主任務計數。 */
    @Query("""
            select t.milestoneId as groupId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   sum(case when t.status <> :done and t.dueDate < :today then 1 else 0 end) as overdue
            from Task t
            where t.milestoneId in :ids and t.parentId is null and t.status <> :cancelled and t.dbMark = 0
            group by t.milestoneId""")
    List<TaskCount> countByMilestone(@Param("ids") Collection<Long> milestoneIds,
            @Param("done") TaskStatus done, @Param("cancelled") TaskStatus cancelled,
            @Param("today") LocalDate today);

    /** 各父任務的子任務計數。 */
    @Query("""
            select t.parentId as groupId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   sum(case when t.status <> :done and t.dueDate < :today then 1 else 0 end) as overdue
            from Task t
            where t.parentId in :ids and t.status <> :cancelled and t.dbMark = 0
            group by t.parentId""")
    List<TaskCount> countByParent(@Param("ids") Collection<Long> parentIds,
            @Param("done") TaskStatus done, @Param("cancelled") TaskStatus cancelled,
            @Param("today") LocalDate today);

    /** 各標籤的任務計數（含子任務）。 */
    @Query("""
            select l as groupId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   sum(case when t.status <> :done and t.dueDate < :today then 1 else 0 end) as overdue
            from Task t join t.labelIds l
            where l in :ids and t.status <> :cancelled and t.dbMark = 0
            group by l""")
    List<TaskCount> countByLabel(@Param("ids") Collection<Long> labelIds,
            @Param("done") TaskStatus done, @Param("cancelled") TaskStatus cancelled,
            @Param("today") LocalDate today);

    /** 各 Sprint 的主任務數與故事點數合計；沒填點數的任務以 0 計。 */
    @Query("""
            select t.sprintId as sprintId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   coalesce(sum(t.storyPoints), 0) as points,
                   coalesce(sum(case when t.status = :done then t.storyPoints else 0 end), 0) as donePoints
            from Task t
            where t.sprintId in :ids and t.parentId is null and t.status <> :cancelled and t.dbMark = 0
            group by t.sprintId""")
    List<SprintCount> countBySprint(@Param("ids") Collection<Long> sprintIds,
            @Param("done") TaskStatus done, @Param("cancelled") TaskStatus cancelled);

    default List<SprintCount> sprintCounts(Collection<Long> sprintIds) {
        return countBySprint(sprintIds, TaskStatus.DONE, TaskStatus.CANCELLED);
    }

    default List<TaskCount> projectCounts(Collection<Long> projectIds) {
        return countByProject(projectIds, TaskStatus.DONE, TaskStatus.CANCELLED, LocalDate.now());
    }

    default List<TaskCount> milestoneCounts(Collection<Long> milestoneIds) {
        return countByMilestone(milestoneIds, TaskStatus.DONE, TaskStatus.CANCELLED,
                LocalDate.now());
    }

    default List<TaskCount> labelCounts(Collection<Long> labelIds) {
        return countByLabel(labelIds, TaskStatus.DONE, TaskStatus.CANCELLED, LocalDate.now());
    }

    default List<TaskCount> subtaskCounts(Collection<Long> parentIds) {
        return countByParent(parentIds, TaskStatus.DONE, TaskStatus.CANCELLED, LocalDate.now());
    }
}
