package dev.jianwii.web.auth;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.common.exception.NotFoundException;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.User;
import dev.jianwii.web.security.OperatorHeaderFilter;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 前端用的登入資訊。登入、登出網址由 Spring Security 提供，見 SecurityConfig。
 */
@RestController
@AllArgsConstructor
@RequestMapping(path = "/api/auth")
public class WebAuthController {
    private final UserApi userApi;

    /**
     * 目前登入者與他的功能權限（permissions）；前端啟動時呼叫，收到 401 代表要導向登入。
     * 前端依 permissions 隱藏沒權限的選單和按鈕，真正擋住請求的是後端的 @PreAuthorize。
     */
    @GetMapping("/me")
    public Result<User> me(@SessionAttribute(OperatorHeaderFilter.SESSION_USER_ID) String userId,
            Authentication authentication) {
        UserQueryParameter parameter = new UserQueryParameter();
        parameter.setIds(List.of(userId));
        User user = userApi.query(parameter).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("查無使用者，id：" + userId));
        // PermissionAuthorityFilter 已把功能權限放進 authorities
        user.setPermissions(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toCollection(TreeSet::new)));
        return Result.success(user);
    }
}
