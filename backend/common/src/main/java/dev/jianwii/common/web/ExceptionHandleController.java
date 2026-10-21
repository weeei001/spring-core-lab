package dev.jianwii.common.web;

import dev.jianwii.common.exception.ProjectException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * 全域例外處理（@RestControllerAdvice）：所有 Controller 丟出的例外都在這裡統一轉成 {@link Result#failure}。 業務錯誤丟
 * {@link ProjectException} 子類別即可。5xx 一律回通用訊息並記錄 log，不把內部錯誤細節回給前端。 由 CommonAutoConfiguration
 * 註冊，各服務不需要再掃描這個 package。
 */
@RestControllerAdvice
public class ExceptionHandleController {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandleController.class);
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter UNEXPECTED_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler(ProjectException.class)
    public ResponseEntity<Result<Void>> handleProjectException(ProjectException e) {
        if (e.getHttpStatus().is5xxServerError()) {
            return unexpectedFailure(e, e.getHttpStatus());
        }
        return ResponseEntity.status(e.getHttpStatus()).body(Result.failure(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Result<Void>> handleOptimisticLock(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.failure("資料衝突", "版本已變更，請重新讀取"));
    }

    /** 例如 id 字串超出 Long 範圍。 */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Result<Void>> handleInvalidIdentifier(NumberFormatException e) {
        return validationFailure("ID 超出有效範圍");
    }

    /** 資料庫約束失敗，例如唯一鍵重複；通常已先在 before* 檢查，這裡處理同時送出的情況。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.failure("資料衝突", "資料重複或仍有關聯資料"));
    }

    /** 其他沒處理到的例外。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        return unexpectedFailure(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failure("NotFound", "找不到此路徑"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleUnsupportedMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Result.failure("不支援的操作", "此資源不支援這個 HTTP 方法"));
    }

    /** @Valid／@Validated 的 request body 驗證失敗。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return validationFailure(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return validationFailure(message);
    }

    /** JSON 格式錯誤或欄位型態不符，例如數字欄位送了文字。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getMostSpecificCause();
        if (cause instanceof MismatchedInputException mismatched
                && !mismatched.getPath().isEmpty()) {
            String field =
                    mismatched.getPath().get(mismatched.getPath().size() - 1).getPropertyName();
            if (field != null && !field.isBlank()) {
                return validationFailure(field + ": 欄位型態不正確");
            }
        }
        return validationFailure("請求格式不正確");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        if (name == null || name.isBlank()) {
            return validationFailure("欄位型態不正確");
        }
        return validationFailure(name + ": 欄位型態不正確");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        return validationFailure(e.getParameterName() + ": 不可為空");
    }

    private ResponseEntity<Result<Void>> validationFailure(String message) {
        String body = StringUtils.hasText(message) ? message : "驗證失敗";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.failure("驗證失敗", body));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Result.failure("檔案過大", "檔案不可超過 10MB"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<Void>> handleMissingPart(MissingServletRequestPartException e) {
        return validationFailure(e.getRequestPartName() + ": 不可為空");
    }

    private ResponseEntity<Result<Void>> unexpectedFailure(Exception e, HttpStatus status) {
        String time = LocalDateTime.now(TAIPEI).format(UNEXPECTED_TIME);
        String message = "發生未預期錯誤，請告訴管理員或工程師（有截圖或是錄影可以更好排除錯誤） " + time;
        log.error(message, e);
        return ResponseEntity.status(status).body(Result.failure("未預期錯誤", message));
    }
}
