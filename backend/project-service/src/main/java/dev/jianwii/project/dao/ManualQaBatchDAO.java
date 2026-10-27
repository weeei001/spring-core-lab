package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ManualQaBatch;

public interface ManualQaBatchDAO extends BaseDAO<ManualQaBatch, Long> {
    boolean existsByProjectId(Long projectId);
}
