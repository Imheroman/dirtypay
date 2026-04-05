package com.dirtypay.global.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 에러 응답 정보.
 *
 * <p>실패한 API 요청에 대한 에러 코드와 메시지를 담는 DTO이다.
 * {@link ApiResponse}의 에러 응답 필드로 사용되어
 * 클라이언트에게 기계 가독성(code)과 인간 가독성(message)을 모두 제공한다.</p>
 *
 * <p>{@code code}는 프로그래매틱하게 처리 가능한 에러 식별자이며,
 * {@code message}는 사용자가 이해할 수 있는 설명 문장이다.</p>
 *
 * <p>예시:</p>
 * <pre>
 * {
 *   "success": false,
 *   "error": {
 *     "code": "INSUFFICIENT_BALANCE",
 *     "message": "잔액이 부족하여 거래할 수 없습니다"
 *   }
 * }
 * </pre>
 *
 * @author kim-young-woong
 * @since 1.0.0
 * @see ApiResponse
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {

    private final String code;
    private final String message;
}
