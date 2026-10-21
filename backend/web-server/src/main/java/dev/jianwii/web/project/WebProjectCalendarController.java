package dev.jianwii.web.project;

import dev.jianwii.api.basicdata.leave.LeaveAbsence;
import dev.jianwii.api.basicdata.leave.LeaveApi;
import dev.jianwii.api.basicdata.leave.LeaveCalendarQuery;
import dev.jianwii.api.project.ProjectMemberApi;
import dev.jianwii.api.project.ProjectMemberQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.exception.BusinessException;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.project.ProjectMember;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 先驗證專案權限，再由服務取得完整成員名單；休假僅揭露人員及日期。 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/project/{projectId}/calendar")
public class WebProjectCalendarController {
    private final ProjectAccessGuard accessGuard;
    private final ProjectMemberApi memberApi;
    private final LeaveApi leaveApi;

    @PreAuthorize("hasAuthority('project:read')")
    @GetMapping("/leaves")
    public Result<List<LeaveAbsence>> leaves(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        accessGuard.requireMember(projectId);
        if (fromDate.isAfter(toDate) || ChronoUnit.DAYS.between(fromDate, toDate) > 61) {
            throw new BusinessException("行事曆日期區間需為 1 至 62 天");
        }
        ProjectMemberQueryParameter query = new ProjectMemberQueryParameter();
        query.setProjectId(projectId);
        query.setSize(100);
        List<LeaveAbsence> absences = new ArrayList<>();
        while (true) {
            PageResponse<ProjectMember> page = memberApi.queryPage(query);
            if (page.getItems().isEmpty()) {
                break;
            }
            absences.addAll(leaveApi.calendar(new LeaveCalendarQuery(
                    page.getItems().stream().map(ProjectMember::getUserId).collect(Collectors.toSet()),
                    fromDate,
                    toDate)));
            if ((long) query.getPage() * query.getSize() >= page.getTotal()) {
                break;
            }
            query.setPage(query.getPage() + 1);
        }
        return Result.success(absences);
    }
}
