package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.ProjectLabel;

import java.util.List;

public interface ProjectLabelDAO extends BaseDAO<ProjectLabel, Long> {

    /** 專案內所有有效標籤；檢查名稱重複、任務的標籤是否屬於此專案時用。 */
    List<ProjectLabel> findByProjectIdAndDbMark(Long projectId, Integer dbMark);
}
