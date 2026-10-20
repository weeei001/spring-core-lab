package dev.jianwii.web.security;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.Permission;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * 每個請求把登入者的功能權限放進 Spring Security，
 * {@code @PreAuthorize("hasAuthority('task:write')")} 才判斷得到。
 * 每次請求都向 basic-data 取得最新權限，管理員調整後，下一個請求就會使用新權限。
 * 只換掉這個請求的 Authentication，不寫回 session。由 SecurityConfig 加進 Spring Security 的 filter 鏈，不是 bean。
 */
public class PermissionAuthorityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(PermissionAuthorityFilter.class);

    /** session 中是否為超級管理員（擁有全部權限），登入成功時由 LoginSuccessHandler 設定。 */
    public static final String SESSION_SUPER_ADMIN = "loginSuperAdmin";

    private final UserApi userApi;
    private final JsonMapper jsonMapper;

    public PermissionAuthorityFilter(UserApi userApi, JsonMapper jsonMapper) {
        this.userApi = userApi;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null
                : session.getAttribute(OperatorHeaderFilter.SESSION_USER_ID);
        if (authentication instanceof OAuth2AuthenticationToken token && userId != null) {
            boolean superAdmin = Boolean.TRUE.equals(session.getAttribute(SESSION_SUPER_ADMIN));
            Set<String> permissions;
            try {
                permissions = superAdmin ? Permission.codes()
                        : Set.copyOf(userApi.permissions(Long.valueOf(userId.toString())));
            } catch (RuntimeException e) {
                // Filter 不會經過 ControllerAdvice；無法確認權限時停止請求，不沿用舊授權。
                log.warn("載入權限失敗，userId：{}", userId, e);
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                jsonMapper.writeValue(response.getOutputStream(),
                        Result.failure("權限查詢失敗", "暫時無法確認權限，請稍後再試"));
                return;
            }
            List<GrantedAuthority> authorities =
                    permissions.stream().<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new OAuth2AuthenticationToken(token.getPrincipal(),
                    authorities, token.getAuthorizedClientRegistrationId()));
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}
