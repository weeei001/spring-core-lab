package dev.jianwii.web.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** 以已驗證的 ID token issuer + sub 識別帳號，固定長度可沿用既有 subject 欄位。 */
final class OidcIdentity {
    private OidcIdentity() {}

    static String subject(OidcUser user) {
        OidcIdToken token = user.getIdToken();
        String identity = token.getIssuer().toExternalForm() + "\0" + token.getSubject();
        try {
            return "oidc:"
                    + HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256")
                                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 必須支援 SHA-256", e);
        }
    }
}
