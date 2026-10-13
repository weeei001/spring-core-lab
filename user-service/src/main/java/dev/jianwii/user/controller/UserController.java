package dev.jianwii.user.controller;

import dev.jianwii.api.user.UserApi;
import dev.jianwii.api.user.UserQueryParameter;
import dev.jianwii.entity.user.User;
import dev.jianwii.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 內部 API，路徑、參數與驗證都定義在 {@link UserApi}，這裡只實作。
 */
@RestController
@AllArgsConstructor
public class UserController implements UserApi {
    private final UserService userService;

    @Override
    public User getById(Long id) {
        return userService.getById(id);
    }

    @Override
    public List<User> getByIds(List<Long> ids) {
        return userService.getByIds(ids);
    }

    @Override
    public List<User> query(UserQueryParameter parameter) {
        return userService.query(parameter);
    }

    @Override
    public User create(User user) {
        return userService.create(user);
    }
}
