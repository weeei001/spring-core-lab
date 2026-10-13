package dev.jianwii.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 目前操作者，透過 {@value #HEADER} header 在服務之間傳遞（值經 URL 編碼，可含中文）。
 *
 * <p>
 * 外部請求的這個 header 會被 gateway 移除，只能由 gateway（登入驗證後）或內部服務呼叫設定；
 * 沒有 header、或不在 HTTP 請求內（排程、啟動程序）時視為 {@value #SYSTEM}。
 */
public final class Operator {

    public static final String HEADER = "X-Operator";
    public static final String SYSTEM = "system";

    private Operator() {
    }

    public static String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String value = request.getHeader(HEADER);
            if (StringUtils.hasText(value)) {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return SYSTEM;
    }

    public static String encode(String operator) {
        return URLEncoder.encode(operator, StandardCharsets.UTF_8);
    }
}
