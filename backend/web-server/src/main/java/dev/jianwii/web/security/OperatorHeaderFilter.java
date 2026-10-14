package dev.jianwii.web.security;

import dev.jianwii.common.web.Operator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 決定 {@value Operator#HEADER}：外部送來的一律忽略（避免偽造），改成 session 中登入者的 user id；
 * 沒登入時沒有這個 header（後端記為 system）。
 * 做法是用 HttpServletRequestWrapper 改寫 header，後續程式（包含 Operator.current()）讀到的都是改寫後的值，
 * 呼叫其他服務時再由 OperatorForwardingInterceptor 帶過去。
 */
@Component
// 要排在 RequestContextFilter（-105）之前，Operator.current() 才會讀到改寫後的請求；
// 之後若加 Spring Session（Integer.MIN_VALUE + 50），這裡要排在它之後才拿得到 session
@Order(-110)
public class OperatorHeaderFilter extends OncePerRequestFilter {

    /** session 中登入者的 user id，登入成功時由 LoginSuccessHandler 設定。 */
    public static final String SESSION_USER_ID = "loginUserId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return isOperator(name) ? operator() : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (!isOperator(name)) {
                    return super.getHeaders(name);
                }
                String operator = operator();
                return operator == null ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(operator));
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>(Collections.list(super.getHeaderNames())
                        .stream().filter(name -> !isOperator(name)).toList());
                if (operator() != null) {
                    names.add(Operator.HEADER);
                }
                return Collections.enumeration(names);
            }

            /** 每次讀取時才查 session，登入、登出後立即生效。 */
            private String operator() {
                HttpSession session = getSession(false);
                Object userId = session == null ? null : session.getAttribute(SESSION_USER_ID);
                return userId == null ? null : Operator.encode(userId.toString());
            }
        }, response);
    }

    private static boolean isOperator(String name) {
        return Operator.HEADER.equalsIgnoreCase(name);
    }
}
