package com.dirtypay.domain.auth.security.jwt;

import com.dirtypay.global.common.dto.ApiResponse;
import com.dirtypay.global.common.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * JWT 인증 EntryPoint 단위 테스트.
 *
 * <p>{@link JwtAuthenticationEntryPoint}의 commence 메서드가
 * 인증 실패 시 올바른 HTTP 상태 코드, Content-Type, 응답 바디를
 * 반환하는지 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationEntryPoint 단위 테스트")
class JwtAuthenticationEntryPointTest {

    @InjectMocks
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Mock
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("commence 메서드 테스트")
    class CommenceTest {

        @Test
        @DisplayName("인증 실패 시 HTTP 상태 코드 401 반환")
        void commence_returns401UnauthorizedStatus() throws IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException authException = new AuthenticationException("Unauthorized") {};

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            assertThat(response.getStatus())
                    .isEqualTo(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        }

        @Test
        @DisplayName("인증 실패 시 Content-Type에 application/json이 포함")
        void commence_setsContentTypeToApplicationJson() throws IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException authException = new AuthenticationException("Unauthorized") {};

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            // response.setCharacterEncoding("UTF-8") 호출로 인해 charset 포함 여부와 관계없이
            // Content-Type이 application/json을 포함하는지 검증한다
            assertThat(response.getContentType())
                    .contains(MediaType.APPLICATION_JSON_VALUE);
        }

        @Test
        @DisplayName("인증 실패 시 ApiResponse.error 형식으로 응답 바디 직렬화")
        void commence_writesApiResponseErrorBody() throws IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            AuthenticationException authException = new AuthenticationException("Unauthorized") {};

            // ObjectMapper가 실제로 ApiResponse<Void>를 writer에게 넘기는지 캡처
            ArgumentCaptor<ApiResponse> bodyCaptor = ArgumentCaptor.forClass(ApiResponse.class);

            // when
            jwtAuthenticationEntryPoint.commence(request, response, authException);

            // then
            // objectMapper.writeValue(writer, body) 호출 시 두 번째 인자가 ApiResponse.error(...) 결과인지 검증
            verify(objectMapper).writeValue(any(java.io.Writer.class), bodyCaptor.capture());

            ApiResponse<?> capturedBody = bodyCaptor.getValue();
            assertThat(capturedBody.isSuccess()).isFalse();
            assertThat(capturedBody.getError()).isNotNull();
            assertThat(capturedBody.getError().getCode())
                    .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
            assertThat(capturedBody.getError().getMessage())
                    .isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
        }
    }
}
