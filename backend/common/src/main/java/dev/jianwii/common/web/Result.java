package dev.jianwii.common.web;

/**
 * 回給前端的統一格式：成功帶 data，失敗帶 errorCode／message。 只有 web-server 回前端時才包 Result；服務之間（XxxApi）成功時直接回物件。
 * 失敗不必手動建立：業務錯誤丟 ProjectException，驗證與型態錯誤也會由 ExceptionHandleController 轉成 failure。
 */
public class Result<T> {

    /** true 成功、false 失敗。 */
    private final boolean result;
    private final T data;
    private final String errorCode;
    private final String message;

    private Result(boolean result, T data, String errorCode, String message) {
        this.result = result;
        this.data = data;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null, null);
    }

    public static Result<Void> success() {
        return new Result<>(true, null, null, null);
    }

    public static <T> Result<T> failure(String errorCode, String message) {
        return new Result<>(false, null, errorCode, message);
    }

    /** 失敗但需要帶結構化資料給前端操作時使用，例如可用格不足時的剩餘格數。 */
    public static <T> Result<T> failure(String errorCode, String message, T data) {
        return new Result<>(false, data, errorCode, message);
    }

    public boolean isResult() {
        return result;
    }

    public T getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
