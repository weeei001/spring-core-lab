package dev.jianwii.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ProjectException {

    public NotFoundException() {
        super("查無資料");
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return "NotFound";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
