package dev.jianwii.web.security;

import dev.jianwii.entity.basicdata.Permission;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 每個請求把登入者的功能權限放進 Spring Security，
 * {@code @PreAuthorize("hasAuthority('task:write')")} 才判斷得到。
 * 權限從 PermissionCache 取（快取 1 分鐘），所以管理員調整權限後不必重新登入。
 * 只換掉這個請求的 Authentication，不寫回 session。由 SecurityConfig 加進 Spring Security 的 filter 鏈，不是 bean。
 */
public class PermissionAuthorityFilter extends OncePerRequestFilter {

    /** session 中是否為超級管理員（擁有全部權限），登入成功時由 LoginSuccessHandler 設定。 */
    public static final String SESSION_SUPER_ADMIN = "loginSuperAdmin";

    private final PermissionCache permissionCache;

    public PermissionAuthorityFilter(PermissionCache permissionCache) {
        this.permissionCache = permissionCache;
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
            Set<String> permissions =
                    superAdmin ? Permission.codes() : permissionCache.get(userId.toString());
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
