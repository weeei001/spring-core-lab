package dev.jianwii.web.controller;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.User;
import dev.jianwii.web.config.OperatorHeaderFilter;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

/**
 * 前端用的登入資訊。登入、登出網址由 Spring Security 提供，見 SecurityConfig。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/auth")
public class WebAuthController {
    private final UserApi userApi;

    /** 目前登入者；前端啟動時呼叫，收到 401 代表要導向登入。 */
    @GetMapping("/me")
    public Result<User> me(@SessionAttribute(OperatorHeaderFilter.SESSION_USER_ID) String userId) {
        UserQueryParameter parameter = new UserQueryParameter();
        parameter.setIds(List.of(userId));
        return Result.success(userApi.query(parameter).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("查無使用者，id：" + userId)));
    }
}
