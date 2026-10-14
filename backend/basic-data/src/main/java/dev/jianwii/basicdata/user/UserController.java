package dev.jianwii.basicdata.user;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.basicdata.User;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 路徑、參數與驗證註解都寫在 {@link UserApi} 介面上，Spring MVC 會沿用，這裡只要轉呼叫 Service。
 */
@RestController
@AllArgsConstructor
public class UserController implements UserApi {
    private final UserService userService;

    @Override
    public List<User> query(UserQueryParameter parameter) {
        return userService.query(parameter);
    }

    @Override
    public PageResponse<User> queryPage(UserQueryParameter parameter) {
        return userService.queryPage(parameter);
    }

    @Override
    public User login(User user) {
        return userService.login(user);
    }

    @Override
    public User updateRoles(Long id, Set<Long> roleIds) {
        return userService.updateRoles(id, roleIds);
    }

    @Override
    public Set<String> permissions(Long id) {
        return userService.permissions(id);
    }
}
