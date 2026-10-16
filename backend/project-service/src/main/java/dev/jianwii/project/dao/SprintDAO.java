package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.Sprint;
import dev.jianwii.entity.project.SprintStatus;

public interface SprintDAO extends BaseDAO<Sprint, Long> {

    /** 同專案已有進行中的 Sprint 時不能再開始另一個。 */
    boolean existsByProjectIdAndStatusAndDbMark(Long projectId, SprintStatus status,
            Integer dbMark);
}
