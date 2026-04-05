package com.dirtypay.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 표준화된 API 응답 래퍼.
 *
 * <p>모든 API 엔드포인트의 응답을 일관된 형식으로 제공한다.
 * 성공 응답에는 {@code success=true}와 데이터를 포함하고,
 * 실패 응답에는 {@code success=false}와 에러 정보를 포함한다.</p>
 *
 * <p>JSON 직렬화 시 null 값은 포함되지 않는다 ({@code JsonInclude.NON_NULL}).</p>
 *
 * <p>제너릭 타입 파라미터 {@code T}는 성공 응답 데이터의 타입을 나타낸다:</p>
 * <ul>
 *   <li>{@code ApiResponse<UserDto>} — 사용자 정보 조회</li>
 *   <li>{@code ApiResponse<List<OrderDto>>} — 주문 목록 조회</li>
 *   <li>{@code ApiResponse<Void>} — 데이터 없는 성공 응답</li>
 * </ul>
 *
 * @param <T> 성공 응답 데이터의 타입
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }
}
