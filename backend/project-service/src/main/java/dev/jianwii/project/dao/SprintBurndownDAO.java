package dev.jianwii.project.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.project.SprintBurndown;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SprintBurndownDAO extends BaseDAO<SprintBurndown, Long> {

    List<SprintBurndown> findBySprintIdOrderByDayAsc(Long sprintId);

    Optional<SprintBurndown> findBySprintIdAndDay(Long sprintId, LocalDate day);
}
