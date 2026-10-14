package dev.jianwii.web.config;

import dev.jianwii.common.web.Operator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * 操作者只能由 web-server（登入後）設定，外部送來的 {@value Operator#HEADER} 一律忽略，避免偽造。
 * 做法是用 HttpServletRequestWrapper 把這個 header 藏起來，後續程式（包含 Operator.current()）都讀不到。
 * 之後加上登入時，改成依登入者回傳這個 header。
 */
@Component
public class OperatorHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return Operator.HEADER.equalsIgnoreCase(name) ? null : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return Operator.HEADER.equalsIgnoreCase(name) ? Collections.emptyEnumeration()
                        : super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return Collections.enumeration(Collections.list(super.getHeaderNames()).stream()
                        .filter(name -> !Operator.HEADER.equalsIgnoreCase(name)).toList());
            }
        }, response);
    }
}
