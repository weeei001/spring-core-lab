package dev.jianwii.common.exception;

import org.springframework.http.HttpStatus;

/** 沒有權限（403），例如帳號已停用。 */
public class ForbiddenException extends ProjectException {

    public ForbiddenException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "無權限";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
