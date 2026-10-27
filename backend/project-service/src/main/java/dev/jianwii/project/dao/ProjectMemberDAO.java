package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ProjectMember;
import dev.jianwii.entity.project.ProjectRole;

public interface ProjectMemberDAO extends BaseDAO<ProjectMember, Long> {

    /** 只看有效成員：被移除（軟刪除）後可以再加入。 */
    boolean existsByProjectIdAndUserId(Long projectId, String userId);

    long countByProjectIdAndRole(Long projectId, ProjectRole role);
}
