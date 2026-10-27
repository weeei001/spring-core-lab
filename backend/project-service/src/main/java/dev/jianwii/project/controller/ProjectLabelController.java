package dev.jianwii.project.controller;

import dev.jianwii.api.project.api.ProjectLabelApi;
import dev.jianwii.api.project.dto.ProjectLabelQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.ProjectLabel;
import dev.jianwii.project.service.ProjectLabelService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路徑、參數與驗證註解都寫在 {@link ProjectLabelApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class ProjectLabelController implements ProjectLabelApi {
    private final ProjectLabelService projectLabelService;

    @Override
    public List<ProjectLabel> query(ProjectLabelQueryParameter parameter) {
        return projectLabelService.query(parameter);
    }

    @Override
    public PageResponse<ProjectLabel> queryPage(ProjectLabelQueryParameter parameter) {
        return projectLabelService.queryPage(parameter);
    }

    @Override
    public ProjectLabel create(ProjectLabel label) {
        return projectLabelService.create(label);
    }

    @Override
    public ProjectLabel update(Long id, ProjectLabel label) {
        return projectLabelService.update(id, label);
    }

    @Override
    public void delete(Long id) {
        projectLabelService.delete(id);
    }
}
