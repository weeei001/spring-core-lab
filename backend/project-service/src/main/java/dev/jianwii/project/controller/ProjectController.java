package dev.jianwii.project.controller;

import dev.jianwii.api.project.ProjectApi;
import dev.jianwii.api.project.ProjectQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.Project;
import dev.jianwii.project.service.ProjectService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路徑、參數與驗證註解都寫在 {@link ProjectApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class ProjectController implements ProjectApi {
    private final ProjectService projectService;

    @Override
    public List<Project> query(ProjectQueryParameter parameter) {
        return projectService.query(parameter);
    }

    @Override
    public PageResponse<Project> queryPage(ProjectQueryParameter parameter) {
        return projectService.queryPage(parameter);
    }

    @Override
    public Project create(Project project) {
        return projectService.create(project);
    }

    @Override
    public Project update(Long id, Project project) {
        return projectService.update(id, project);
    }

    @Override
    public void delete(Long id) {
        projectService.delete(id);
    }
}
