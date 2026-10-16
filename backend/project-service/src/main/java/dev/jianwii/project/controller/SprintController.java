package dev.jianwii.project.controller;

import dev.jianwii.api.project.SprintApi;
import dev.jianwii.api.project.SprintQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.Sprint;
import dev.jianwii.entity.project.SprintBurndown;
import dev.jianwii.project.service.SprintService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link SprintApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class SprintController implements SprintApi {
    private final SprintService sprintService;

    @Override
    public List<Sprint> query(SprintQueryParameter parameter) {
        return sprintService.query(parameter);
    }

    @Override
    public PageResponse<Sprint> queryPage(SprintQueryParameter parameter) {
        return sprintService.queryPage(parameter);
    }

    @Override
    public Sprint create(Sprint sprint) {
        return sprintService.create(sprint);
    }

    @Override
    public Sprint update(Long id, Sprint sprint) {
        return sprintService.update(id, sprint);
    }

    @Override
    public void delete(Long id) {
        sprintService.delete(id);
    }

    @Override
    public Sprint start(Long id) {
        return sprintService.start(id);
    }

    @Override
    public Sprint close(Long id, Long moveToSprintId) {
        return sprintService.close(id, moveToSprintId);
    }

    @Override
    public List<SprintBurndown> burndown(Long id) {
        return sprintService.burndown(id);
    }
}
