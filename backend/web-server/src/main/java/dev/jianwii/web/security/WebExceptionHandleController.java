package dev.jianwii.web.security;

import dev.jianwii.common.web.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * web-server 專用的例外處理：{@code @PreAuthorize} 不通過時回 403 Result。
 * 要排在 common 的 ExceptionHandleController 之前，否則會被它當成未預期錯誤回 500。
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandleController {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.failure("無權限", "沒有此功能的權限"));
    }
}
