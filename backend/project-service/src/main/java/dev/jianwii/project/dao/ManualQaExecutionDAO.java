package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ManualQaExecution;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualQaExecutionDAO extends BaseDAO<ManualQaExecution, Long> {
    boolean existsByItemIdAndDbMark(Long itemId, Integer dbMark);

    /** 每個項目的最新一筆紀錄：executedAt 最大，相同時取 id 最大。 */
    @Query("""
            select e from ManualQaExecution e
            where e.itemId in :itemIds and e.dbMark = :dbMark
              and not exists (
                select 1 from ManualQaExecution n
                where n.itemId = e.itemId and n.dbMark = :dbMark
                  and (n.executedAt > e.executedAt or (n.executedAt = e.executedAt and n.id > e.id)))
            """)
    List<ManualQaExecution> findLatestByItemIds(
            @Param("itemIds") Collection<Long> itemIds, @Param("dbMark") Integer dbMark);

    Page<ManualQaExecution> findByBugTaskIdAndDbMark(Long bugTaskId, Integer dbMark, Pageable page);
}
