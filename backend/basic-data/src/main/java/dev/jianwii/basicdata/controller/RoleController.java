package dev.jianwii.basicdata.controller;

import dev.jianwii.api.basicdata.RoleApi;
import dev.jianwii.api.basicdata.RoleQueryParameter;
import dev.jianwii.basicdata.service.RoleService;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.basicdata.Role;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路徑、參數與驗證註解都寫在 {@link RoleApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class RoleController implements RoleApi {
    private final RoleService roleService;

    @Override
    public List<Role> query(RoleQueryParameter parameter) {
        return roleService.query(parameter);
    }

    @Override
    public PageResponse<Role> queryPage(RoleQueryParameter parameter) {
        return roleService.queryPage(parameter);
    }

    @Override
    public Role create(Role role) {
        return roleService.create(role);
    }

    @Override
    public Role update(Long id, Role role) {
        return roleService.update(id, role);
    }

    @Override
    public void delete(Long id) {
        roleService.delete(id);
    }
}
