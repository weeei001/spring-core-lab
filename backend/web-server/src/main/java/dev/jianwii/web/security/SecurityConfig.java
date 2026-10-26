package dev.jianwii.web.security;

import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.common.web.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * 登入設定：web-server 以 OAuth2 Client 身分向 OIDC 提供者登入，token 留在後端，瀏覽器只拿到 session cookie。
 *
 * <ul>
 * <li>登入：前端整頁跳轉到 {@value #LOGIN_URI}/{registrationId}，登入完成導回 {@code app.frontend-url}。</li>
 * <li>登出：前端 POST {@value #LOGOUT_URI}（要帶 CSRF header），回傳提供者的登出網址；不支援 OIDC 登出時直接返回前端。</li>
 * <li>API：提供者清單可匿名讀取；其他 API 要登入，未登入回 401 Result。</li>
 * <li>權限：Controller 以 {@code @PreAuthorize("hasAuthority('task:write')")} 檢查功能權限， 沒有權限回 403
 * Result；權限由 PermissionAuthorityFilter 每個請求載入。</li>
 * <li>CSRF：登入靠 cookie，所以要防 CSRF。後端把 token 放在 XSRF-TOKEN cookie， 前端送出 POST／PATCH／DELETE 時放進
 * X-XSRF-TOKEN header（axios 會自動處理）。</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
// 啟用 @PreAuthorize
@EnableMethodSecurity
public class SecurityConfig {

    private static final String LOGIN_URI = "/auth/login";
    /** 各提供者使用獨立回呼網址。 */
    private static final String CALLBACK_URI = "/auth/callback/*";

    private static final String LOGOUT_URI = "/auth/logout";

    /** OAuth2 token 與登入身分一起存入 JDBC session，重啟後仍可讀取與登出。 */
    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    /** 只有獨立管理 port 的 health 可匿名讀取，主服務 port 不提供此入口。 */
    @Bean
    @Order(0)
    SecurityFilterChain healthFilterChain(HttpSecurity http, @Value("${management.server.port}") int managementPort)
            throws Exception {
        http.securityMatcher(request -> request.getLocalPort() == managementPort)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .denyAll());
        return http.build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginSuccessHandler loginSuccessHandler,
            UserApi userApi,
            ClientRegistrationRepository clientRegistrationRepository,
            JsonMapper jsonMapper,
            @Value("${app.super-admin-subjects:}") Set<String> superAdminSubjects,
            @Value("${app.frontend-url}") String frontendUrl)
            throws Exception {
        // 依登入使用的 registration 查詢 end_session_endpoint；Google 沒有該端點。
        // axios 無法跨站跟隨登出，回傳 JSON 讓前端整頁跳轉。
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository) {
                    @Override
                    public void onLogoutSuccess(
                            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
                            throws IOException {
                        writeJson(
                                jsonMapper,
                                response,
                                HttpStatus.OK,
                                Result.success(determineTargetUrl(request, response, authentication)));
                    }
                };
        logoutSuccessHandler.setPostLogoutRedirectUri(frontendUrl);
        logoutSuccessHandler.setDefaultTargetUrl(frontendUrl);

        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator", "/actuator/**")
                        .denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/providers")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2Login(login -> login.authorizationEndpoint(endpoint -> endpoint.baseUri(LOGIN_URI))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri(CALLBACK_URI))
                        .successHandler(loginSuccessHandler)
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect(UriComponentsBuilder.fromUriString(frontendUrl)
                                        .queryParam("loginError", "登入失敗，請重新嘗試")
                                        .build()
                                        .encode()
                                        .toUriString())))
                .logout(logout -> logout.logoutUrl(LOGOUT_URI).logoutSuccessHandler(logoutSuccessHandler))
                .csrf(csrf -> csrf.spa())
                // 先確認帳號有效，再做 CSRF 與功能授權；停用帳號一致回 401。
                .addFilterBefore(
                        new PermissionAuthorityFilter(userApi, jsonMapper, superAdminSubjects), CsrfFilter.class)
                // 預設會把未登入的請求導向登入頁；API 改回 JSON，由前端決定怎麼處理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, e) ->
                                writeJson(jsonMapper, response, HttpStatus.UNAUTHORIZED, Result.failure("未登入", "請先登入")))
                        // filter 層被拒絕，目前只有 CSRF token 不符；@PreAuthorize 不通過由
                        // WebExceptionHandleController 處理
                        .accessDeniedHandler((request, response, e) -> writeJson(
                                jsonMapper,
                                response,
                                HttpStatus.FORBIDDEN,
                                Result.failure("無權限", "請求驗證失敗，請重新整理頁面後再試"))));
        return http.build();
    }

    private static void writeJson(
            JsonMapper jsonMapper, HttpServletResponse response, HttpStatus status, Result<?> result)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), result);
    }
}
