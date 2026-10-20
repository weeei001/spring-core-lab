package dev.jianwii.project.service;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.entity.project.ProjectMember;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/** 任務與工時共用的專案範圍；EXISTS 不受成員數或分頁上限影響。 */
final class ProjectMemberScope {
    private ProjectMemberScope() {}

    static <T> Specification<T> forUser(String userId) {
        return (root, query, builder) -> {
            if (userId == null) {
                return builder.conjunction();
            }
            Subquery<Long> members = query.subquery(Long.class);
            Root<ProjectMember> member = members.from(ProjectMember.class);
            members.select(member.get("id")).where(
                    builder.equal(member.get("projectId"), root.get("projectId")),
                    builder.equal(member.get("userId"), userId),
                    builder.equal(member.get("dbMark"), BaseEntity.VALID));
            return builder.exists(members);
        };
    }
}
