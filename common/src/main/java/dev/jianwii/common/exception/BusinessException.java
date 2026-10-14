package dev.jianwii.common.exception;

import org.springframework.http.HttpStatus;

/** 業務規則不符（400），例如庫存不足、格式不支援。 */
public class BusinessException extends ProjectException {

    public BusinessException(String message) {
        super(message);
    }

    @Override
    public String getErrorCode() {
        return "業務驗證失敗";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
