package com.dirtypay.global.exception;

import com.dirtypay.global.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 로직 검증 실패 시 발생하는 예외.
 *
 * <p>{@link RuntimeException}을 상속하여 비검사 예외(unchecked exception)로 처리되며,
 * 핵심 애플리케이션 비즈니스 규칙 위반을 나타낸다.</p>
 *
 * <p>각 예외는 {@link ErrorCode}를 반드시 포함하므로,
 * 호출자는 에러 코드로부터 클라이언트에게 반환할 HTTP 상태 코드 및 메시지를 결정할 수 있다.
 * {@link ErrorCode}는 데이터베이스 제약조건 위반, 비즈니스 규칙 위반, 권한 부족 등을
 * 구분 가능하게 정의한다.</p>
 *
 * <p>예시:</p>
 * <pre>
 * // 충분한 잔액 부족
 * throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
 *
 * // 사용자 정의 메시지 포함
 * throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문 금액이 음수입니다");
 *
 * // 근본 원인 연쇄
 * catch (SQLException e) {
 *     throw new BusinessException(ErrorCode.DATABASE_ERROR, e);
 * }
 * </pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 * @see ErrorCode
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
