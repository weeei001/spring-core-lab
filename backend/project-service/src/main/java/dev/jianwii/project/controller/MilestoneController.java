package dev.jianwii.project.controller;

import dev.jianwii.api.project.MilestoneApi;
import dev.jianwii.api.project.MilestoneQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.Milestone;
import dev.jianwii.project.service.MilestoneService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link MilestoneApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class MilestoneController implements MilestoneApi {
    private final MilestoneService milestoneService;

    @Override
    public List<Milestone> query(MilestoneQueryParameter parameter) {
        return milestoneService.query(parameter);
    }

    @Override
    public PageResponse<Milestone> queryPage(MilestoneQueryParameter parameter) {
        return milestoneService.queryPage(parameter);
    }

    @Override
    public Milestone create(Milestone milestone) {
        return milestoneService.create(milestone);
    }

    @Override
    public Milestone update(Long id, Milestone milestone) {
        return milestoneService.update(id, milestone);
    }

    @Override
    public void delete(Long id) {
        milestoneService.delete(id);
    }
}
