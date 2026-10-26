package dev.jianwii.web.security;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/** MySQL 是 OIDC 設定來源；啟動時讀取一次，修改 SQL 後重啟生效。 */
@Configuration(proxyBeanMethods = false)
public class OidcClientConfiguration {
    @Bean
    InMemoryClientRegistrationRepository clientRegistrationRepository(
            JdbcTemplate jdbc, @Value("${app.public-url}") String publicUrl) {
        var registrations = jdbc.query("""
                SELECT registration_id, client_name, issuer_uri, client_id, client_secret,
                       scopes, client_authentication_method
                FROM oidc_client_registration
                WHERE enabled = TRUE
                ORDER BY registration_id
                """, (rs, row) -> {
            String id = rs.getString("registration_id");
            Assert.isTrue(id.matches("[a-z0-9][a-z0-9-]*"), "OIDC registration_id 必須是小寫英數與連字號");
            String clientId = rs.getString("client_id");
            String secret = rs.getString("client_secret");
            Assert.hasText(clientId, "OIDC client_id 不可空白");
            Assert.hasText(secret, "OIDC client_secret 不可空白");
            String[] scopes = StringUtils.tokenizeToStringArray(rs.getString("scopes"), ", \t\r\n");
            Assert.isTrue(Arrays.asList(scopes).contains("openid"), "OIDC scopes 必須包含 openid");
            String method = rs.getString("client_authentication_method");
            Assert.isTrue(
                    "client_secret_basic".equals(method) || "client_secret_post".equals(method),
                    "OIDC client_authentication_method 只支援 client_secret_basic 或 client_secret_post");
            return ClientRegistrations.fromIssuerLocation(rs.getString("issuer_uri"))
                    .registrationId(id)
                    .clientName(rs.getString("client_name"))
                    .clientId(clientId)
                    .clientSecret(secret)
                    .clientAuthenticationMethod(new ClientAuthenticationMethod(method))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(publicUrl + "/auth/callback/{registrationId}")
                    .scope(scopes)
                    .userNameAttributeName("sub")
                    .build();
        });
        Assert.state(!registrations.isEmpty(), "oidc_client_registration 至少需要一筆已啟用的 OIDC 設定；請先以 SQL 建立 Google 設定");
        return new InMemoryClientRegistrationRepository(registrations);
    }
}
