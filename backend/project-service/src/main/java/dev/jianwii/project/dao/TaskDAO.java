package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ProjectStatus;
import dev.jianwii.entity.project.Task;
import dev.jianwii.entity.project.TaskStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 統計查詢只算有效、未取消的任務；done 為完成數，overdue 為過了到期日仍未完成的數量。
 */
public interface TaskDAO extends BaseDAO<Task, Long> {

    /** 只提醒仍在有效專案中的成員；完成、取消或無期限的任務不納入。 */
    @Query("""
            select new dev.jianwii.project.dao.TaskDigestItem(t.id, t.code, t.title, t.assigneeId, t.dueDate)
            from Task t join Project p on p.id = t.projectId
            where p.status in :projectStatuses
              and t.status in :taskStatuses and t.dueDate <= :until
              and t.assigneeId is not null
              and exists (select m.id from ProjectMember m
                  where m.projectId = t.projectId and m.userId = t.assigneeId)
            order by t.assigneeId, t.dueDate, t.id
            """)
    List<TaskDigestItem> findDigestItems(
            @Param("until") LocalDate until,
            @Param("taskStatuses") Collection<TaskStatus> taskStatuses,
            @Param("projectStatuses") Collection<ProjectStatus> projectStatuses);

    /** 看板一欄（同專案、同狀態）的任務，依排序。 */
    List<Task> findByProjectIdAndStatusOrderBySortOrderAscIdAsc(Long projectId, TaskStatus status);

    /** 欄位最後一張的排序值；欄位是空的時回 null。 */
    @Query("select max(t.sortOrder) from Task t where t.projectId = :projectId and t.status = :status")
    Long findMaxSortOrder(@Param("projectId") Long projectId, @Param("status") TaskStatus status);

    /** 專案內所有有效任務；檢查前置任務是否成環時用。 */
    List<Task> findByProjectId(Long projectId);

    /** 移除成員時，找出指派給該成員的任務。 */
    List<Task> findByProjectIdAndAssigneeId(Long projectId, String assigneeId);

    /** 前置任務含此任務的有效任務；刪除任務時一併移除關聯。 */
    List<Task> findByPredecessorIdsContaining(Long predecessorId);

    /** 移除成員時，找出該成員關注的任務。 */
    List<Task> findByProjectIdAndWatcherIdsContaining(Long projectId, String watcherId);

    /** 標籤含此標籤的有效任務；刪除標籤時一併移除關聯。 */
    List<Task> findByLabelIdsContaining(Long labelId);

    boolean existsByProjectId(Long projectId);

    boolean existsByParentId(Long parentId);

    /** 各專案的主任務計數。 */
    @Query("""
            select t.projectId as groupId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   sum(case when t.status <> :done and t.dueDate < :today then 1 else 0 end) as overdue
            from Task t
            where t.projectId in :ids and t.parentId is null and t.status <> :cancelled
            group by t.projectId""")
    List<TaskCount> countByProject(
            @Param("ids") Collection<Long> projectIds,
            @Param("done") TaskStatus done,
            @Param("cancelled") TaskStatus cancelled,
            @Param("today") LocalDate today);

    /** 各父任務的子任務計數。 */
    @Query("""
            select t.parentId as groupId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   sum(case when t.status <> :done and t.dueDate < :today then 1 else 0 end) as overdue
            from Task t
            where t.parentId in :ids and t.status <> :cancelled
            group by t.parentId""")
    List<TaskCount> countByParent(
            @Param("ids") Collection<Long> parentIds,
            @Param("done") TaskStatus done,
            @Param("cancelled") TaskStatus cancelled,
            @Param("today") LocalDate today);

    /** 各標籤的任務計數（含子任務）。 */
    @Query("""
            select l as groupId, count(t) as total,
                   sum(case when t.status = :done then 1 else 0 end) as done,
                   sum(case when t.status <> :done and t.dueDate < :today then 1 else 0 end) as overdue
            from Task t join t.labelIds l
            where l in :ids and t.status <> :cancelled
            group by l""")
    List<TaskCount> countByLabel(
            @Param("ids") Collection<Long> labelIds,
            @Param("done") TaskStatus done,
            @Param("cancelled") TaskStatus cancelled,
            @Param("today") LocalDate today);

    default List<TaskCount> projectCounts(Collection<Long> projectIds) {
        return countByProject(projectIds, TaskStatus.DONE, TaskStatus.CANCELLED, LocalDate.now());
    }

    default List<TaskCount> labelCounts(Collection<Long> labelIds) {
        return countByLabel(labelIds, TaskStatus.DONE, TaskStatus.CANCELLED, LocalDate.now());
    }

    default List<TaskCount> subtaskCounts(Collection<Long> parentIds) {
        return countByParent(parentIds, TaskStatus.DONE, TaskStatus.CANCELLED, LocalDate.now());
    }
}
