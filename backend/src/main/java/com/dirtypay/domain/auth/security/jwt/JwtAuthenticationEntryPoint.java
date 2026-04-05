package com.dirtypay.domain.auth.security.jwt;

import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.common.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 인증 실패 시 401 Unauthorized 응답을 반환하는 EntryPoint.
 *
 * <p>인증되지 않은 사용자가 인증이 필요한 엔드포인트에 접근할 때
 * {@link ErrorCode#UNAUTHORIZED}에 해당하는 JSON 응답을 반환한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 인증 실패 시 401 응답을 JSON 형식으로 반환한다.
     *
     * @param request       HTTP 요청
     * @param response      HTTP 응답
     * @param authException 인증 예외
     * @throws IOException IO 예외
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        this.objectMapper.writeValue(response.getWriter(), body);
    }
}
