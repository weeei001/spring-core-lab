package dev.jianwii.web.basicdata;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.User;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 前端用的使用者查詢：呼叫 basic-data 的 {@link UserApi}，結果包成 Result。 query 用來把 createId、transId
 * 換成名字，登入就能用；其餘為權限管理，需要 role:manage。 登入同步（login）、載入權限（permissions）只給 web-server 內部使用，不開放。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/basic-data/user")
public class WebUserController {
    private final UserApi userApi;

    @PostMapping("/query")
    public Result<List<User>> query(@Valid @RequestBody UserQueryParameter parameter) {
        return Result.success(userApi.query(parameter));
    }

    @PreAuthorize("hasAuthority('role:manage')")
    @PostMapping("/queryPage")
    public Result<PageResponse<User>> queryPage(@Valid @RequestBody UserQueryParameter parameter) {
        return Result.success(userApi.queryPage(parameter));
    }

    /** 指派角色：整批取代；下一個請求就會依新的角色檢查權限。 */
    @PreAuthorize("hasAuthority('role:manage')")
    @PutMapping("/{id}/roles")
    public Result<User> updateRoles(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        return Result.success(userApi.updateRoles(id, roleIds));
    }
}
