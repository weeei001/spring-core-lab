package dev.jianwii.basicdata.controller;

import dev.jianwii.api.basicdata.api.UserApi;
import dev.jianwii.api.basicdata.dto.UserAuthorization;
import dev.jianwii.api.basicdata.dto.UserQueryParameter;
import dev.jianwii.basicdata.service.UserService;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.basicdata.User;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

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

    @Override
    public UserAuthorization authorization(Long id) {
        return userService.authorization(id);
    }
}
