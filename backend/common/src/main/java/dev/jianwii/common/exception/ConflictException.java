package dev.jianwii.common.exception;

import org.springframework.http.HttpStatus;

/** 與既有資料衝突（409），例如代碼重複。 */
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
