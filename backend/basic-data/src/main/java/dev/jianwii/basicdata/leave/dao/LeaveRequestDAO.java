package dev.jianwii.basicdata.leave.dao;

import dev.jianwii.api.basicdata.dto.LeaveAbsence;
import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.leave.LeaveRequest;
import dev.jianwii.entity.basicdata.leave.LeaveStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestDAO extends BaseDAO<LeaveRequest, Long> {
    @Query("""
            select new dev.jianwii.api.basicdata.leave.LeaveAbsence(r.applicantId, r.startDate, r.endDate)
            from LeaveRequest r
            where r.status = :status and r.applicantId in :userIds
              and r.startDate <= :toDate and r.endDate >= :fromDate
            order by r.startDate, r.id
            """)
    List<LeaveAbsence> calendar(
            @Param("userIds") Set<String> userIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") LeaveStatus status);
}
