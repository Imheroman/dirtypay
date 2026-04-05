package com.dirtypay.global.exception;

import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.common.enums.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * GlobalExceptionHandler 단위 테스트.
 *
 * <p>각 예외 유형에 대해 올바른 HTTP 상태 코드와 ErrorCode가
 * 응답에 포함되는지 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        // 핸들러를 직접 인스턴스화하여 Spring Context 없이 단위 테스트 수행
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleBusinessException 테스트")
    class HandleBusinessExceptionTest {

        @Test
        @DisplayName("UNAUTHORIZED ErrorCode 예외 처리 시 401 상태 코드와 올바른 에러 코드를 반환한다")
        void handleBusinessException_unauthorized() {
            // given
            BusinessException exception = new BusinessException(ErrorCode.UNAUTHORIZED);

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleBusinessException(exception);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isFalse();
            assertThat(result.getBody().getError()).isNotNull();
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
        }

        @Test
        @DisplayName("FORBIDDEN ErrorCode 예외 처리 시 403 상태 코드를 반환한다")
        void handleBusinessException_forbidden() {
            // given
            BusinessException exception = new BusinessException(ErrorCode.FORBIDDEN);

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleBusinessException(exception);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode());
        }

        @Test
        @DisplayName("커스텀 메시지와 함께 BusinessException 처리 시 해당 메시지가 응답에 포함된다")
        void handleBusinessException_customMessage() {
            // given
            String customMessage = "사용자 정의 오류 메시지";
            BusinessException exception = new BusinessException(ErrorCode.INVALID_TOKEN, customMessage);

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleBusinessException(exception);

            // then
            assertThat(result.getBody().getError().getMessage()).isEqualTo(customMessage);
        }

        @Test
        @DisplayName("RESOURCE_NOT_FOUND ErrorCode 예외 처리 시 404 상태 코드를 반환한다")
        void handleBusinessException_resourceNotFound() {
            // given
            BusinessException exception = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleBusinessException(exception);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
        }
    }

    @Nested
    @DisplayName("handleMethodArgumentNotValidException 테스트")
    class HandleMethodArgumentNotValidExceptionTest {

        @Test
        @DisplayName("MethodArgumentNotValidException 처리 시 400 상태 코드와 INVALID_INPUT_VALUE 코드를 반환한다")
        void handleMethodArgumentNotValidException_returnsInvalidInputValue() {
            // given
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("request", "email", "이메일 형식이 올바르지 않습니다");

            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

            // when
            ResponseEntity<ApiResponse<Void>> result =
                    handler.handleMethodArgumentNotValidException(exception);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isFalse();
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        }

        @Test
        @DisplayName("FieldError가 있으면 필드명과 메시지를 조합한 에러 메시지를 반환한다")
        void handleMethodArgumentNotValidException_fieldErrorMessageFormat() {
            // given
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("request", "email", "이메일 형식이 올바르지 않습니다");

            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

            // when
            ResponseEntity<ApiResponse<Void>> result =
                    handler.handleMethodArgumentNotValidException(exception);

            // then
            assertThat(result.getBody().getError().getMessage())
                    .isEqualTo("email: 이메일 형식이 올바르지 않습니다");
        }

        @Test
        @DisplayName("FieldError가 없으면 기본 INVALID_INPUT_VALUE 메시지를 반환한다")
        void handleMethodArgumentNotValidException_noFieldError_defaultMessage() {
            // given
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);

            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of());

            // when
            ResponseEntity<ApiResponse<Void>> result =
                    handler.handleMethodArgumentNotValidException(exception);

            // then
            assertThat(result.getBody().getError().getMessage())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        }
    }

    @Nested
    @DisplayName("handleBindException 테스트")
    class HandleBindExceptionTest {

        @Test
        @DisplayName("BindException 처리 시 400 상태 코드와 INVALID_INPUT_VALUE 코드를 반환한다")
        void handleBindException_returnsInvalidInputValue() {
            // given
            // BindException은 mock 생성이 가능하나 생성자가 복잡하므로 mock으로 처리
            org.springframework.validation.BindException bindException =
                    mock(org.springframework.validation.BindException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("request", "name", "이름은 필수입니다");

            given(bindException.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleBindException(bindException);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        }

        @Test
        @DisplayName("BindException의 FieldError 메시지가 응답에 포함된다")
        void handleBindException_fieldErrorMessageIncluded() {
            // given
            org.springframework.validation.BindException bindException =
                    mock(org.springframework.validation.BindException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("request", "name", "이름은 필수입니다");

            given(bindException.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError));

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleBindException(bindException);

            // then
            assertThat(result.getBody().getError().getMessage())
                    .isEqualTo("name: 이름은 필수입니다");
        }
    }

    @Nested
    @DisplayName("handleHttpRequestMethodNotSupportedException 테스트")
    class HandleHttpRequestMethodNotSupportedExceptionTest {

        @Test
        @DisplayName("허용되지 않은 HTTP 메서드 예외 처리 시 405 상태 코드를 반환한다")
        void handleHttpRequestMethodNotSupportedException_returnsMethodNotAllowed() {
            // given
            HttpRequestMethodNotSupportedException exception =
                    new HttpRequestMethodNotSupportedException("DELETE");

            // when
            ResponseEntity<ApiResponse<Void>> result =
                    handler.handleHttpRequestMethodNotSupportedException(exception);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isFalse();
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.getCode());
        }

        @Test
        @DisplayName("METHOD_NOT_ALLOWED 응답에 미리 정의된 에러 메시지가 포함된다")
        void handleHttpRequestMethodNotSupportedException_messageFromErrorCode() {
            // given
            HttpRequestMethodNotSupportedException exception =
                    new HttpRequestMethodNotSupportedException("PATCH");

            // when
            ResponseEntity<ApiResponse<Void>> result =
                    handler.handleHttpRequestMethodNotSupportedException(exception);

            // then
            assertThat(result.getBody().getError().getMessage())
                    .isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.getMessage());
        }
    }

    @Nested
    @DisplayName("handleLockException 테스트")
    class HandleLockExceptionTest {

        @Test
        @DisplayName("PessimisticLockingFailureException 처리 시 409 Conflict와 LOCK_ACQUISITION_FAILED 코드를 반환한다")
        void handleLockException_returns409WithLockAcquisitionFailed() {
            // given
            org.springframework.dao.PessimisticLockingFailureException exception =
                    new org.springframework.dao.PessimisticLockingFailureException("Lock acquisition failed");

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleLockException(exception);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isFalse();
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.LOCK_ACQUISITION_FAILED.getCode());
        }

        @Test
        @DisplayName("LOCK_ACQUISITION_FAILED 응답에 사용자 친화적 메시지가 포함된다")
        void handleLockException_returnsUserFriendlyMessage() {
            // given
            org.springframework.dao.PessimisticLockingFailureException exception =
                    new org.springframework.dao.PessimisticLockingFailureException("timeout");

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleLockException(exception);

            // then
            assertThat(result.getBody().getError().getMessage())
                    .isEqualTo(ErrorCode.LOCK_ACQUISITION_FAILED.getMessage());
        }
    }

    @Nested
    @DisplayName("handleException 테스트")
    class HandleExceptionTest {

        @Test
        @DisplayName("예상치 못한 Exception 처리 시 500 상태 코드를 반환한다")
        void handleException_returnsInternalServerError() {
            // given
            Exception exception = new RuntimeException("예상치 못한 오류");

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleException(exception);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isFalse();
            assertThat(result.getBody().getError().getCode())
                    .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        }

        @Test
        @DisplayName("handleException 응답에 INTERNAL_SERVER_ERROR 메시지가 포함된다")
        void handleException_messageFromErrorCode() {
            // given
            Exception exception = new NullPointerException();

            // when
            ResponseEntity<ApiResponse<Void>> result = handler.handleException(exception);

            // then
            assertThat(result.getBody().getError().getMessage())
                    .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        }
    }
}
