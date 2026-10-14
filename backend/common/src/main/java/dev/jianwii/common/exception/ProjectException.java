package dev.jianwii.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 專案自訂例外的基底。丟出後由 ExceptionHandleController 轉成 Result.failure 回傳，
 * HTTP 狀態取 {@link #getHttpStatus()}、errorCode 取 {@link #getErrorCode()}。
 * 業務邏輯直接丟子類別即可，交易會自動 rollback。
 */
public abstract class ProjectException extends RuntimeException {

    public ProjectException(String message) {
        super(message);
    }

    public ProjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract String getErrorCode();

    public HttpStatus getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
