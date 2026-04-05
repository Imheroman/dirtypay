package com.dirtypay.domain.auth.service;

import com.dirtypay.domain.auth.dto.request.LoginRequest;
import com.dirtypay.domain.auth.dto.request.RefreshTokenRequest;
import com.dirtypay.domain.auth.dto.request.SignupRequest;
import com.dirtypay.domain.auth.dto.response.TokenValidationResponse;
import com.dirtypay.domain.auth.entity.RefreshToken;
import com.dirtypay.domain.auth.repository.RefreshTokenRepository;
import com.dirtypay.domain.auth.security.jwt.JwtProperties;
import com.dirtypay.domain.auth.security.jwt.JwtTokenProvider;
import com.dirtypay.domain.auth.service.AuthService.LoginResult;
import com.dirtypay.domain.member.dto.response.MemberResponse;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.wallet.service.WalletService;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * AuthService 단위 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private WalletService walletService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTest {

        @Test
        @DisplayName("회원가입 성공")
        void signup_success() {
            // given
            SignupRequest request = new SignupRequest();
            ReflectionTestUtils.setField(request, "email", "test@test.com");
            ReflectionTestUtils.setField(request, "password", "password123");
            ReflectionTestUtils.setField(request, "name", "테스트");

            Member savedMember = createMember(1L, "test@test.com", "테스트");

            given(memberRepository.existsByEmail("test@test.com"))
                    .willReturn(false);
            given(passwordEncoder.encode("password123"))
                    .willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class)))
                    .willReturn(savedMember);

            // when
            MemberResponse response = authService.signup(request);

            // then
            assertThat(response.getEmail()).isEqualTo("test@test.com");
            assertThat(response.getName()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("중복 이메일로 회원가입 시 DUPLICATE_EMAIL(409) 예외 발생")
        void signup_duplicateEmail() {
            // given
            SignupRequest request = new SignupRequest();
            ReflectionTestUtils.setField(request, "email", "existing@test.com");
            ReflectionTestUtils.setField(request, "password", "password123");
            ReflectionTestUtils.setField(request, "name", "테스트");

            given(memberRepository.existsByEmail("existing@test.com"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("로그인 성공 - 신규 Refresh Token 저장")
        void login_success_newRefreshToken() {
            // given
            LoginRequest request = new LoginRequest();
            ReflectionTestUtils.setField(request, "email", "test@test.com");
            ReflectionTestUtils.setField(request, "password", "password123");

            Member member = createMember(1L, "test@test.com", "테스트");
            ReflectionTestUtils.setField(member, "password", "encodedPassword");

            given(memberRepository.findByEmail("test@test.com"))
                    .willReturn(Optional.of(member));
            given(passwordEncoder.matches("password123", "encodedPassword"))
                    .willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString()))
                    .willReturn("accessToken");
            given(jwtTokenProvider.createRefreshToken(anyLong(), anyString()))
                    .willReturn("refreshToken");
            given(jwtProperties.getAccessTokenExpiration())
                    .willReturn(3600000L);
            given(jwtProperties.getRefreshTokenExpiration())
                    .willReturn(604800000L);
            given(refreshTokenRepository.findByMemberId(1L))
                    .willReturn(Optional.empty());
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willReturn(null);

            // when
            LoginResult result = authService.login(request);

            // then
            assertThat(result.accessToken()).isEqualTo("accessToken");
            assertThat(result.refreshToken()).isEqualTo("refreshToken");
            assertThat(result.accessTokenExpiresIn()).isEqualTo(3600L);
            assertThat(result.refreshTokenExpiresIn()).isEqualTo(604800L);

            verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("로그인 성공 - 기존 Refresh Token 갱신")
        void login_success_updateRefreshToken() {
            // given
            LoginRequest request = new LoginRequest();
            ReflectionTestUtils.setField(request, "email", "test@test.com");
            ReflectionTestUtils.setField(request, "password", "password123");

            Member member = createMember(1L, "test@test.com", "테스트");
            ReflectionTestUtils.setField(member, "password", "encodedPassword");

            RefreshToken existingToken = RefreshToken.builder()
                    .memberId(1L)
                    .token("oldRefreshToken")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            given(memberRepository.findByEmail("test@test.com"))
                    .willReturn(Optional.of(member));
            given(passwordEncoder.matches("password123", "encodedPassword"))
                    .willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyLong(), anyString(), anyString()))
                    .willReturn("newAccessToken");
            given(jwtTokenProvider.createRefreshToken(anyLong(), anyString()))
                    .willReturn("newRefreshToken");
            given(jwtProperties.getAccessTokenExpiration())
                    .willReturn(3600000L);
            given(jwtProperties.getRefreshTokenExpiration())
                    .willReturn(604800000L);
            given(refreshTokenRepository.findByMemberId(1L))
                    .willReturn(Optional.of(existingToken));

            // when
            LoginResult result = authService.login(request);

            // then
            assertThat(result.accessToken()).isEqualTo("newAccessToken");
            assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
            assertThat(existingToken.getToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
        void login_memberNotFound() {
            // given
            LoginRequest request = new LoginRequest();
            ReflectionTestUtils.setField(request, "email", "notfound@test.com");
            ReflectionTestUtils.setField(request, "password", "password123");

            given(memberRepository.findByEmail("notfound@test.com"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
        void login_wrongPassword() {
            // given
            LoginRequest request = new LoginRequest();
            ReflectionTestUtils.setField(request, "email", "test@test.com");
            ReflectionTestUtils.setField(request, "password", "wrongPassword");

            Member member = createMember(1L, "test@test.com", "테스트");
            ReflectionTestUtils.setField(member, "password", "encodedPassword");

            given(memberRepository.findByEmail("test@test.com"))
                    .willReturn(Optional.of(member));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("토큰 갱신 테스트")
    class RefreshAccessTokenTest {

        @Test
        @DisplayName("토큰 갱신 성공")
        void refreshAccessToken_success() {
            // given
            RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");

            RefreshToken storedToken = RefreshToken.builder()
                    .memberId(1L)
                    .token("validRefreshToken")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            Member member = createMember(1L, "test@test.com", "테스트");

            given(jwtTokenProvider.validateToken("validRefreshToken"))
                    .willReturn(true);
            given(refreshTokenRepository.findByToken("validRefreshToken"))
                    .willReturn(Optional.of(storedToken));
            given(memberRepository.findById(1L))
                    .willReturn(Optional.of(member));
            given(jwtTokenProvider.createAccessToken(1L, "test@test.com", "ROLE_USER"))
                    .willReturn("newAccessToken");
            given(jwtTokenProvider.createRefreshToken(1L, "test@test.com"))
                    .willReturn("newRefreshToken");
            given(jwtProperties.getAccessTokenExpiration())
                    .willReturn(3600000L);
            given(jwtProperties.getRefreshTokenExpiration())
                    .willReturn(604800000L);

            // when
            LoginResult result = authService.refreshAccessToken(request);

            // then
            assertThat(result.accessToken()).isEqualTo("newAccessToken");
            assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 갱신 시 예외 발생")
        void refreshAccessToken_invalidToken() {
            // given
            RefreshTokenRequest request = new RefreshTokenRequest("invalidToken");

            given(jwtTokenProvider.validateToken("invalidToken"))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("유효하지 않은");
        }

        @Test
        @DisplayName("DB에 없는 토큰으로 갱신 시 예외 발생")
        void refreshAccessToken_tokenNotFound() {
            // given
            RefreshTokenRequest request = new RefreshTokenRequest("notFoundToken");

            given(jwtTokenProvider.validateToken("notFoundToken"))
                    .willReturn(true);
            given(refreshTokenRepository.findByToken("notFoundToken"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }

        @Test
        @DisplayName("만료된 토큰으로 갱신 시 예외 발생")
        void refreshAccessToken_expiredToken() {
            // given
            RefreshTokenRequest request = new RefreshTokenRequest("expiredToken");

            RefreshToken expiredToken = RefreshToken.builder()
                    .memberId(1L)
                    .token("expiredToken")
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();

            given(jwtTokenProvider.validateToken("expiredToken"))
                    .willReturn(true);
            given(refreshTokenRepository.findByToken("expiredToken"))
                    .willReturn(Optional.of(expiredToken));

            // when & then
            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("만료");

            verify(refreshTokenRepository, times(1)).delete(expiredToken);
        }
    }

    @Nested
    @DisplayName("토큰 유효성 검증 테스트")
    class ValidateRefreshTokenTest {

        @Test
        @DisplayName("유효한 토큰 검증 성공")
        void validateRefreshToken_valid() {
            // given
            RefreshToken validToken = RefreshToken.builder()
                    .memberId(1L)
                    .token("validToken")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            given(jwtTokenProvider.validateToken("validToken"))
                    .willReturn(true);
            given(refreshTokenRepository.findByToken("validToken"))
                    .willReturn(Optional.of(validToken));

            // when
            TokenValidationResponse response = authService.validateRefreshToken("validToken");

            // then
            assertThat(response.isValid()).isTrue();
            assertThat(response.getExpiresInSeconds()).isGreaterThan(0);
        }

        @Test
        @DisplayName("만료된 토큰 검증")
        void validateRefreshToken_expired() {
            // given
            RefreshToken expiredToken = RefreshToken.builder()
                    .memberId(1L)
                    .token("expiredToken")
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();

            given(jwtTokenProvider.validateToken("expiredToken"))
                    .willReturn(true);
            given(refreshTokenRepository.findByToken("expiredToken"))
                    .willReturn(Optional.of(expiredToken));

            // when
            TokenValidationResponse response = authService.validateRefreshToken("expiredToken");

            // then
            assertThat(response.isValid()).isFalse();
            assertThat(response.getMessage()).contains("expired");
        }

        @Test
        @DisplayName("유효하지 않은 형식의 토큰 검증")
        void validateRefreshToken_invalidFormat() {
            // given
            given(jwtTokenProvider.validateToken("invalidToken"))
                    .willReturn(false);

            // when
            TokenValidationResponse response = authService.validateRefreshToken("invalidToken");

            // then
            assertThat(response.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공 - jti가 유효하면 블랙리스트에 등록한다")
        void logout_success() {
            // given
            Long memberId = 1L;
            String accessToken = "mock-access-token";
            String jti = "test-jti-uuid-1234";

            given(jwtTokenProvider.extractJtiSafely(accessToken)).willReturn(jti);
            given(jwtTokenProvider.getRemainingExpiryMillis(accessToken)).willReturn(3000L);

            // when
            authService.logout(memberId, accessToken);

            // then
            verify(refreshTokenRepository, times(1)).deleteAllByMemberId(memberId);
            verify(tokenBlacklistService, times(1)).blacklistAccessToken(jti, 3000L);
        }

        @Test
        @DisplayName("로그아웃 성공 - jti가 null이면 블랙리스트 등록을 건너뛴다")
        void logout_nullJti_skipsBlacklist() {
            // given
            Long memberId = 1L;
            String accessToken = "legacy-token-without-jti";

            given(jwtTokenProvider.extractJtiSafely(accessToken)).willReturn(null);

            // when
            authService.logout(memberId, accessToken);

            // then
            verify(refreshTokenRepository, times(1)).deleteAllByMemberId(memberId);
            verify(tokenBlacklistService, times(0)).blacklistAccessToken(anyString(), anyLong());
        }

        @Test
        @DisplayName("로그아웃 성공 - accessToken이 null이면 NPE 없이 refresh token만 삭제한다")
        void logout_nullAccessToken_onlyDeletesRefreshToken() {
            // given
            Long memberId = 1L;

            // when
            authService.logout(memberId, null);

            // then
            verify(refreshTokenRepository, times(1)).deleteAllByMemberId(memberId);
            verify(jwtTokenProvider, times(0)).extractJtiSafely(any());
            verify(tokenBlacklistService, times(0)).blacklistAccessToken(anyString(), anyLong());
        }

        @Test
        @DisplayName("로그아웃 성공 - 만료된 accessToken이어도 500 에러 없이 refresh token 삭제 및 블랙리스트 등록한다")
        void logout_expiredAccessToken_doesNotThrow() {
            // given
            Long memberId = 1L;
            String expiredAccessToken = "expired-access-token";
            String jti = "expired-jti-uuid-5678";

            // extractJtiSafely는 만료 토큰에서도 jti를 반환 (ExpiredJwtException 내부 claims 접근)
            given(jwtTokenProvider.extractJtiSafely(expiredAccessToken)).willReturn(jti);
            // 만료 토큰이므로 남은 유효 시간은 0
            given(jwtTokenProvider.getRemainingExpiryMillis(expiredAccessToken)).willReturn(0L);

            // when — ExpiredJwtException이 전파되지 않아야 한다
            authService.logout(memberId, expiredAccessToken);

            // then
            verify(refreshTokenRepository, times(1)).deleteAllByMemberId(memberId);
            // 잔여 시간 0으로 블랙리스트 등록 호출 (TTL 0 = 즉시 만료 처리)
            verify(tokenBlacklistService, times(1)).blacklistAccessToken(jti, 0L);
        }

        @Test
        @DisplayName("토큰으로 로그아웃 성공")
        void logoutByToken_success() {
            // given
            String refreshToken = "someRefreshToken";

            // when
            authService.logoutByToken(refreshToken);

            // then
            verify(refreshTokenRepository, times(1)).deleteByToken(refreshToken);
        }
    }

    private Member createMember(Long id, String email, String name) {
        Member member = Member.builder()
                .email(email)
                .password("password123")
                .name(name)
                .role(MemberRole.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
