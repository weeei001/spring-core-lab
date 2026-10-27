package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ManualQaItem;
import java.util.List;

public interface ManualQaItemDAO extends BaseDAO<ManualQaItem, Long> {
    List<ManualQaItem> findByBatchIdOrderByIdAsc(Long batchId);
}
