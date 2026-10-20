package dev.jianwii.web.security;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.common.exception.RemoteServiceException;
import dev.jianwii.entity.basicdata.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * authentik 登入成功後：把登入者同步到 basic-data 的 user 表，user id 存進 session，再導回前端。
 * 同步失敗（帳號已停用、basic-data 沒啟動）就不算登入，清掉 session 並帶 loginError 參數導回前端。
 * subject 列在 {@code app.super-admin-subjects} 的是超級管理員，擁有全部權限，用來指派第一批角色。
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginSuccessHandler.class);

    private final UserApi userApi;
    private final String frontendUrl;

    public LoginSuccessHandler(UserApi userApi, @Value("${app.frontend-url}") String frontendUrl) {
        this.userApi = userApi;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        HttpSession session = request.getSession();
        // 同一個瀏覽器換帳號登入時，先清掉上一個登入者，同步時才不會記成他
        session.removeAttribute(OperatorHeaderFilter.SESSION_USER_ID);
        session.removeAttribute("loginSuperAdmin");
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        User user = new User();
        user.setSubject(oidcUser.getSubject());
        user.setUsername(oidcUser.getPreferredUsername());
        user.setName(oidcUser.getFullName());
        user.setEmail(oidcUser.getEmail());
        try {
            User loginUser = userApi.login(user);
            session.setAttribute(OperatorHeaderFilter.SESSION_USER_ID,
                    String.valueOf(loginUser.getId()));
            response.sendRedirect(frontendUrl);
        } catch (RuntimeException e) {
            log.warn("登入同步失敗，subject：{}", oidcUser.getSubject(), e);
            session.invalidate();
            SecurityContextHolder.clearContext();
            // 4xx（例如帳號已停用）可以直接給使用者看；其他錯誤不透露內部細節
            String message = e instanceof RemoteServiceException remote
                    && remote.getHttpStatus().is4xxClientError() ? remote.getMessage()
                            : "登入失敗，請稍後再試";
            response.sendRedirect(UriComponentsBuilder.fromUriString(frontendUrl)
                    .queryParam("loginError", message).encode().toUriString());
        }
    }
}
