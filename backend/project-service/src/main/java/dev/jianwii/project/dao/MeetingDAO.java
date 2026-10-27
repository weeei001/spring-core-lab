package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.Meeting;

public interface MeetingDAO extends BaseDAO<Meeting, Long> {
    boolean existsByProjectId(Long projectId);
}
