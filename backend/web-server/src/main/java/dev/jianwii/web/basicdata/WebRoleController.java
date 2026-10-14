package dev.jianwii.web.basicdata;

import dev.jianwii.api.basicdata.RoleApi;
import dev.jianwii.api.basicdata.RoleQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.basicdata.Role;
import dev.jianwii.web.security.PermissionCache;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端用的角色維護：呼叫 basic-data 的 {@link RoleApi}，結果包成 Result。
 * 整個 Controller 都需要 role:manage 權限（標在類別上，每個方法都適用）。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/basic-data/role")
@PreAuthorize("hasAuthority('role:manage')")
public class WebRoleController {
    private final RoleApi roleApi;
    private final PermissionCache permissionCache;

    /** 所有可勾選的功能權限，依 group 分組顯示。 */
    @GetMapping("/permissions")
    public Result<List<Permission>> permissions() {
        return Result.success(List.of(Permission.values()));
    }

    @PostMapping("/query")
    public Result<List<Role>> query(@Valid @RequestBody RoleQueryParameter parameter) {
        return Result.success(roleApi.query(parameter));
    }

    @PostMapping("/queryPage")
    public Result<PageResponse<Role>> queryPage(@Valid @RequestBody RoleQueryParameter parameter) {
        return Result.success(roleApi.queryPage(parameter));
    }

    @PostMapping
    public Result<Role> create(@Validated(ValidGroup.Create.class) @RequestBody Role role) {
        return Result.success(roleApi.create(role));
    }

    @PatchMapping("/{id}")
    public Result<Role> update(@PathVariable Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Role role) {
        Role updated = roleApi.update(id, role);
        // 角色權限改了，擁有這個角色的使用者都受影響
        permissionCache.clear();
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleApi.delete(id);
        return Result.success();
    }
}
