package dev.jianwii.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 目前操作者的使用者 id（user 資料表的 id），寫進審計欄位 createId、transId；透過 {@value #HEADER} header 在服務之間傳遞 （值經 URL
 * 編碼，含特殊字元也能放進 header）。
 *
 * <p>
 * 前端送來的這個 header 會被 web-server 的 OperatorHeaderFilter 忽略，只能由 web-server（登入驗證後）或服務之間的呼叫設定。
 */
public final class Operator {

    public static final String HEADER = "X-Operator";
    public static final String SYSTEM = "system";

    private Operator() {}

    /** 從目前 HTTP 請求的 header 取得操作者 id；沒有 header 或不在 HTTP 請求內（排程、啟動程序）時回 {@value #SYSTEM}。 */
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
