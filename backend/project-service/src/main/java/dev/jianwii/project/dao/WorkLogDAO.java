package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.WorkLog;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkLogDAO extends BaseDAO<WorkLog, Long> {

    /** 各任務已登記的工時合計。 */
    @Query("""
            select w.taskId as taskId, sum(w.hours) as hours
            from WorkLog w
            where w.taskId in :ids
            group by w.taskId""")
    List<TaskHours> sumHoursByTask(@Param("ids") Collection<Long> taskIds);

    /** 某人某天的工時合計，不含 excludeId 這筆（修改時排除自己；新增時給 0）。 */
    @Query("""
            select coalesce(sum(w.hours), 0)
            from WorkLog w
            where w.userId = :userId and w.workDate = :workDate and w.id <> :excludeId""")
    BigDecimal sumHoursOfDay(
            @Param("userId") String userId, @Param("workDate") LocalDate workDate, @Param("excludeId") Long excludeId);
}
