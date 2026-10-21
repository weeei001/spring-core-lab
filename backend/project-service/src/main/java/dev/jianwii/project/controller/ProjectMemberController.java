package dev.jianwii.project.controller;

import dev.jianwii.api.project.ProjectMemberApi;
import dev.jianwii.api.project.ProjectMemberQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.project.ProjectMember;
import dev.jianwii.project.service.ProjectMemberService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路徑、參數與驗證註解都寫在 {@link ProjectMemberApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class ProjectMemberController implements ProjectMemberApi {
    private final ProjectMemberService projectMemberService;

    @Override
    public List<ProjectMember> query(ProjectMemberQueryParameter parameter) {
        return projectMemberService.query(parameter);
    }

    @Override
    public PageResponse<ProjectMember> queryPage(ProjectMemberQueryParameter parameter) {
        return projectMemberService.queryPage(parameter);
    }

    @Override
    public ProjectMember create(ProjectMember member) {
        return projectMemberService.create(member);
    }

    @Override
    public ProjectMember update(Long id, ProjectMember member) {
        return projectMemberService.update(id, member);
    }

    @Override
    public void delete(Long id) {
        projectMemberService.delete(id);
    }
}
