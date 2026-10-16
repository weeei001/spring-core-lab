package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ProjectMember;
import dev.jianwii.entity.project.ProjectRole;

public interface ProjectMemberDAO extends BaseDAO<ProjectMember, Long> {

    /** 只看有效成員：被移除（軟刪除）後可以再加入。 */
    boolean existsByProjectIdAndUserIdAndDbMark(Long projectId, String userId, Integer dbMark);

    long countByProjectIdAndRoleAndDbMark(Long projectId, ProjectRole role, Integer dbMark);
}
