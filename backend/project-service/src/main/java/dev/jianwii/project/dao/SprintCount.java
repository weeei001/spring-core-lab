package dev.jianwii.project.dao;

/** Sprint 的任務與點數合計（TaskDAO.countBySprint 的結果）；只算主任務，取消的不計。 */
public interface SprintCount {

    Long getSprintId();

    Long getTotal();

    Long getDone();

    Long getPoints();

    Long getDonePoints();
}
