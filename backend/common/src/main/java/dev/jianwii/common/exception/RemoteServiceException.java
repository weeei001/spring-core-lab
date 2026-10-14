package dev.jianwii.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 呼叫其他服務失敗時，把對方回傳的 {@code Result.failure} 還原成例外，讓 errorCode、message 與 HTTP 狀態原樣傳回前端。
 * 對方是 5xx 時由 ExceptionHandleController 改回未預期錯誤，不會把遠端內部訊息洩漏出去。
 */
public class RemoteServiceException extends ProjectException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public RemoteServiceException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
