package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ManualQaExecution;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManualQaExecutionDAO extends BaseDAO<ManualQaExecution, Long> {
    boolean existsByItemIdAndDbMark(Long itemId, Integer dbMark);

    Optional<ManualQaExecution> findFirstByItemIdAndDbMarkOrderByExecutedAtDescIdDesc(Long itemId, Integer dbMark);

    Page<ManualQaExecution> findByItemIdAndDbMark(Long itemId, Integer dbMark, Pageable page);

    Page<ManualQaExecution> findByBugTaskIdAndDbMark(Long bugTaskId, Integer dbMark, Pageable page);
}
