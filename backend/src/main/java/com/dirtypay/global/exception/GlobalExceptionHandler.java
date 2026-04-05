package com.dirtypay.global.exception;

import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.common.dto.ErrorResponse;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.lock.LockAcquisitionFailedException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("BusinessException: {}", e.getMessage());
        } else {
            log.warn("BusinessException: {}", e.getMessage());
        }
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                message
        );
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("BindException: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                message
        );
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    /**
     * {@code @Validated} 컨트롤러에서 발생하는 파라미터 제약 조건 위반 예외를 처리한다.
     *
     * <p>{@code @RequestParam} 또는 {@code @PathVariable}에 {@code @Size}, {@code @NotBlank} 등
     * Bean Validation 어노테이션이 적용된 경우 위반 시 이 핸들러가 호출된다.</p>
     *
     * @param e 제약 조건 위반 예외
     * @return 400 Bad Request 응답
     * @author kim-young-woong
     * @since 1.0.0
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                message
        );
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    /**
     * HikariCP 커넥션 풀 고갈 예외를 처리한다.
     *
     * <p>DB 커넥션 풀이 소진되어 트랜잭션을 열 수 없을 때 503 Service Unavailable을 반환한다.
     * RFC 7231에 따라 일시적 과부하 상태는 503이 의미론적으로 정확하며,
     * Retry-After 헤더로 클라이언트가 재시도 가능함을 안내한다.</p>
     *
     * @param e 커넥션 획득 실패 예외
     * @return 503 Service Unavailable 응답
     * @author kim-young-woong
     * @since 1.0.0
     */
    @ExceptionHandler(CannotCreateTransactionException.class)
    protected ResponseEntity<ApiResponse<Void>> handleCannotCreateTransactionException(
            CannotCreateTransactionException e) {
        log.error("DB connection pool exhausted: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.SERVICE_UNAVAILABLE;
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "5");
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .headers(headers)
                .body(ApiResponse.error(errorResponse));
    }

    /**
     * 분산 락 획득 실패 예외를 처리한다.
     *
     * <p>Redisson 분산 락 대기 시간 초과로 인해 락 획득에 실패했을 때 409 Conflict를 반환한다.
     * 클라이언트는 Retry-After 헤더에 명시된 시간(초) 후 재시도해야 한다.</p>
     *
     * @param e 분산 락 획득 실패 예외
     * @return 409 Conflict 응답
     * @author kim-young-woong
     * @since 1.0.0
     */
    @ExceptionHandler(LockAcquisitionFailedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleLockAcquisitionFailedException(
            LockAcquisitionFailedException e) {
        log.warn("Distributed lock acquisition failed: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.LOCK_ACQUISITION_FAILED;
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Retry-After", "3");
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .headers(headers)
                .body(ApiResponse.error(errorResponse));
    }

    /**
     * 비관적 락 획득 실패 예외를 처리한다.
     *
     * <p>동시 요청으로 인해 DB 비관적 락 획득에 실패했을 때 409 Conflict를 반환한다.</p>
     *
     * @param e 락 예외
     * @return 409 Conflict 응답
     * @author kim-young-woong
     * @since 1.0.0
     */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    protected ResponseEntity<ApiResponse<Void>> handleLockException(PessimisticLockingFailureException e) {
        log.warn("Lock acquisition failed: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.LOCK_ACQUISITION_FAILED;
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    /**
     * Spring Security {@code @PreAuthorize} / {@code @PostAuthorize} 인가 거부 예외를 처리한다.
     *
     * <p>Spring Security 6에서 {@code AccessDeniedException} 대신
     * {@link AuthorizationDeniedException}이 사용된다.
     * {@code @RestControllerAdvice}에서 처리하지 않으면 catch-all 핸들러가 500을 반환하므로
     * 명시적으로 403 Forbidden을 반환한다.</p>
     *
     * @param e 인가 거부 예외
     * @return 403 Forbidden 응답
     * @author kim-young-woong
     * @since 1.0.0
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(
            AuthorizationDeniedException e) {
        log.warn("AuthorizationDeniedException: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Exception: ", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }
}
