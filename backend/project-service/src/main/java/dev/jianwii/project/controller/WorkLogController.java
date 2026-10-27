package dev.jianwii.project.controller;

import dev.jianwii.api.project.api.WorkLogApi;
import dev.jianwii.api.project.dto.WorkLogQueryParameter;
import dev.jianwii.api.project.dto.WorkLogSummary;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.WorkLog;
import dev.jianwii.project.service.WorkLogService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路徑、參數與驗證註解都寫在 {@link WorkLogApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class WorkLogController implements WorkLogApi {
    private final WorkLogService workLogService;

    @Override
    public List<WorkLog> query(WorkLogQueryParameter parameter) {
        return workLogService.query(parameter);
    }

    @Override
    public PageResponse<WorkLog> queryPage(WorkLogQueryParameter parameter) {
        return workLogService.queryPage(parameter);
    }

    @Override
    public WorkLog create(WorkLog workLog) {
        return workLogService.create(workLog);
    }

    @Override
    public WorkLog update(Long id, WorkLog workLog) {
        return workLogService.update(id, workLog);
    }

    @Override
    public void delete(Long id) {
        workLogService.delete(id);
    }

    @Override
    public List<WorkLogSummary> summary(WorkLogQueryParameter parameter) {
        return workLogService.summary(parameter);
    }
}
