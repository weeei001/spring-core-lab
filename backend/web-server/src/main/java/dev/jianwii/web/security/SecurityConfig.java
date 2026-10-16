package dev.jianwii.web.security;

import dev.jianwii.common.web.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登入設定：web-server 以 OAuth2 Client 身分向 authentik 登入，token 留在後端，瀏覽器只拿到 session cookie。
 *
 * <ul>
 * <li>登入：前端整頁跳轉到 {@value #LOGIN_URI}/authentik，登入完成導回 {@code app.frontend-url}。</li>
 * <li>登出：前端 POST {@value #LOGOUT_URI}（要帶 CSRF header），回傳 authentik 的登出網址，
 * 前端再整頁跳轉過去，一併登出 authentik 後導回前端。</li>
 * <li>API：一律要登入，未登入回 401 Result，前端收到後導向登入網址。</li>
 * <li>權限：Controller 以 {@code @PreAuthorize("hasAuthority('task:write')")} 檢查功能權限，
 * 沒有權限回 403 Result；權限由 PermissionAuthorityFilter 每個請求載入。</li>
 * <li>CSRF：登入靠 cookie，所以要防 CSRF。後端把 token 放在 XSRF-TOKEN cookie，
 * 前端送出 POST／PATCH／DELETE 時放進 X-XSRF-TOKEN header（axios 會自動處理）。</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
// 啟用 @PreAuthorize
@EnableMethodSecurity
public class SecurityConfig {

    private static final String LOGIN_URI = "/auth/login";
    /** authentik 登入完成後導回的網址，要和 authentik 設定的 redirect URI 一致。 */
    private static final String CALLBACK_URI = "/auth/callback";
    private static final String LOGOUT_URI = "/auth/logout";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            LoginSuccessHandler loginSuccessHandler, PermissionCache permissionCache,
            ClientRegistrationRepository clientRegistrationRepository, JsonMapper jsonMapper,
            @Value("${app.frontend-url}") String frontendUrl) throws Exception {
        // 登出時也結束 authentik 的登入狀態，否則下次登入會直接通過。
        // 前端用 axios 呼叫，302 導向 authentik（跨網域）跟不過去，所以改成回 JSON，由前端自己跳轉
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository) {
                    @Override
                    public void onLogoutSuccess(HttpServletRequest request,
                            HttpServletResponse response, Authentication authentication)
                            throws IOException {
                        writeJson(jsonMapper, response, HttpStatus.OK, Result.success(
                                determineTargetUrl(request, response, authentication)));
                    }
                };
        logoutSuccessHandler.setPostLogoutRedirectUri(frontendUrl);

        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint.baseUri(LOGIN_URI))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri(CALLBACK_URI))
                        .successHandler(loginSuccessHandler))
                .logout(logout -> logout.logoutUrl(LOGOUT_URI)
                        .logoutSuccessHandler(logoutSuccessHandler))
                .csrf(csrf -> csrf.spa())
                // 在檢查權限之前，把登入者的功能權限放進 Spring Security
                .addFilterBefore(new PermissionAuthorityFilter(permissionCache),
                        AuthorizationFilter.class)
                // 預設會把未登入的請求導向登入頁；API 改回 JSON，由前端決定怎麼處理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, e) -> writeJson(jsonMapper,
                                response, HttpStatus.UNAUTHORIZED, Result.failure("未登入", "請先登入")))
                        // filter 層被拒絕，目前只有 CSRF token 不符；@PreAuthorize 不通過由 WebExceptionHandleController 處理
                        .accessDeniedHandler((request, response, e) -> writeJson(jsonMapper,
                                response, HttpStatus.FORBIDDEN,
                                Result.failure("無權限", "請求驗證失敗，請重新整理頁面後再試"))));
        return http.build();
    }

    private static void writeJson(JsonMapper jsonMapper, HttpServletResponse response,
            HttpStatus status, Result<?> result) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), result);
    }
}
