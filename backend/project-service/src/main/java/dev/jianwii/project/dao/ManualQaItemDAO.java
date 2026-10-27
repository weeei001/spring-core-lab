package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ManualQaItem;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManualQaItemDAO extends BaseDAO<ManualQaItem, Long> {
    List<ManualQaItem> findByBatchIdAndDbMarkOrderByIdAsc(Long batchId, Integer dbMark);

    Page<ManualQaItem> findByBatchIdAndDbMark(Long batchId, Integer dbMark, Pageable page);
}
