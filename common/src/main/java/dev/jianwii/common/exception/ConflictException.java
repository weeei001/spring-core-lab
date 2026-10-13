package dev.jianwii.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ProjectException {

    public ConflictException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "資料衝突";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
