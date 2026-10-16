package dev.jianwii.project.service;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.entity.project.Project;
import dev.jianwii.project.dao.ProjectDAO;
import dev.jianwii.project.dao.ProjectMemberDAO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 專案底下各資料（成員、里程碑、任務、留言、工時）共用的檢查。
 * 獨立成元件而非放在 ProjectService，避免各 Service 互相注入。
 */
@Component
@AllArgsConstructor
public class ProjectRules {
    private final ProjectDAO projectDAO;
    private final ProjectMemberDAO projectMemberDAO;

    /** 專案必須存在，且未結案、未取消（結案、取消的專案唯讀）。 */
    public Project requireOpen(Long projectId) {
        Project project = projectDAO.findById(projectId)
                .filter(entity -> entity.getDbMark() == BaseEntity.VALID)
                .orElseThrow(() -> new BusinessException("專案不存在：" + projectId));
        if (project.getStatus().isClosed()) {
            throw new BusinessException("專案已結案或取消，請先重新開啟");
        }
        return project;
    }

    /** userId 必須是專案的有效成員，例如任務的指派人。 */
    public void requireMember(Long projectId, String userId) {
        if (!projectMemberDAO.existsByProjectIdAndUserIdAndDbMark(projectId, userId,
                BaseEntity.VALID)) {
            throw new BusinessException("不是此專案的成員：" + userId);
        }
    }
}
