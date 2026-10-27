package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ManualQaBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManualQaBatchDAO extends BaseDAO<ManualQaBatch, Long> {
    boolean existsByProjectIdAndDbMark(Long projectId, Integer dbMark);

    Page<ManualQaBatch> findByProjectIdAndDbMark(Long projectId, Integer dbMark, Pageable page);
}
