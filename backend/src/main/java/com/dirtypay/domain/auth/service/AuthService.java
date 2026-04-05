package com.dirtypay.domain.auth.service;

import com.dirtypay.domain.auth.dto.request.LoginRequest;
import com.dirtypay.domain.auth.dto.request.RefreshTokenRequest;
import com.dirtypay.domain.auth.dto.request.SignupRequest;
import com.dirtypay.domain.auth.dto.response.TokenValidationResponse;
import com.dirtypay.domain.auth.entity.RefreshToken;
import com.dirtypay.domain.auth.repository.RefreshTokenRepository;
import com.dirtypay.domain.auth.security.jwt.JwtProperties;
import com.dirtypay.domain.auth.security.jwt.JwtTokenProvider;
import com.dirtypay.domain.member.dto.response.MemberResponse;
import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.domain.wallet.service.WalletService;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 인증 서비스.
 *
 * <p>회원가입, 로그인, 토큰 갱신, 로그아웃 기능을 제공한다.
 * 비밀번호는 BCrypt 알고리즘으로 암호화되며,
 * 로그인 성공 시 JWT 토큰을 발급한다.</p>
 *
 * <p>Access Token은 HttpOnly Cookie로 관리되며,
 * Refresh Token은 DB에 저장하여 폐기 기능을 지원한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final WalletService walletService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 새로운 회원을 등록한다.
     *
     * @param request 회원가입 요청 DTO
     * @return 등록된 회원 정보 응답 DTO
     * @throws BusinessException 이메일이 이미 사용 중인 경우
     */
    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (this.memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(this.passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .profileImage(request.getProfileImage())
                .role(MemberRole.USER)
                .build();

        Member savedMember = this.memberRepository.save(member);

        this.walletService.createWallet(savedMember.getId());

        return MemberResponse.from(savedMember);
    }

    /**
     * 이메일과 비밀번호로 로그인한다.
     *
     * <p>로그인 성공 시:</p>
     * <ul>
     *   <li>Access Token 생성 (Cookie로 전달될 예정)</li>
     *   <li>Refresh Token 생성 및 DB 저장</li>
     *   <li>기존 Refresh Token이 있으면 갱신</li>
     * </ul>
     *
     * @param request 로그인 요청 DTO
     * @return 로그인 결과 (Access Token, Refresh Token, 만료 시간)
     * @throws BusinessException 이메일 또는 비밀번호가 잘못된 경우
     */
    @Transactional
    public LoginResult login(LoginRequest request) {
        Member member = this.memberRepository.findByEmail(request.getEmail())
                .filter(m -> this.passwordEncoder.matches(request.getPassword(), m.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 잘못되었습니다."));

        String accessToken = this.jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole().getAuthority());
        String refreshToken = this.jwtTokenProvider.createRefreshToken(member.getId(), member.getEmail());

        // Refresh Token DB 저장 (기존 토큰이 있으면 갱신)
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now()
                .plus(this.jwtProperties.getRefreshTokenExpiration(), ChronoUnit.MILLIS);

        this.refreshTokenRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(refreshToken, refreshTokenExpiresAt),
                        () -> this.refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .memberId(member.getId())
                                        .token(refreshToken)
                                        .expiresAt(refreshTokenExpiresAt)
                                        .build()
                        )
                );

        log.info("Member logged in successfully. memberId={}", member.getId());

        return new LoginResult(
                accessToken,
                refreshToken,
                this.jwtProperties.getAccessTokenExpiration() / 1000,  // 초 단위
                this.jwtProperties.getRefreshTokenExpiration() / 1000, // 초 단위
                member
        );
    }

    /**
     * Refresh Token으로 새 Access Token을 발급한다.
     *
     * <p>검증 과정:</p>
     * <ol>
     *   <li>Refresh Token JWT 유효성 검증</li>
     *   <li>DB에 저장된 토큰과 일치 여부 확인</li>
     *   <li>토큰 만료 여부 확인</li>
     *   <li>회원 존재 여부 확인</li>
     * </ol>
     *
     * @param request Refresh Token 요청 DTO
     * @return 새 Access Token과 Refresh Token
     * @throws BusinessException Refresh Token이 유효하지 않은 경우
     */
    @Transactional
    public LoginResult refreshAccessToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // JWT 유효성 검증
        if (!this.jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다.");
        }

        // DB에서 토큰 조회
        RefreshToken storedToken = this.refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh Token을 찾을 수 없습니다."));

        // 만료 여부 확인
        if (storedToken.isExpired()) {
            this.refreshTokenRepository.delete(storedToken);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh Token이 만료되었습니다.");
        }

        // 회원 조회
        Long memberId = storedToken.getMemberId();
        Member member = this.memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 새 토큰 발급
        String newAccessToken = this.jwtTokenProvider.createAccessToken(member.getId(), member.getEmail(), member.getRole().getAuthority());
        String newRefreshToken = this.jwtTokenProvider.createRefreshToken(member.getId(), member.getEmail());

        // Refresh Token 갱신
        LocalDateTime newExpiresAt = LocalDateTime.now()
                .plus(this.jwtProperties.getRefreshTokenExpiration(), ChronoUnit.MILLIS);
        storedToken.updateToken(newRefreshToken, newExpiresAt);

        log.info("Access token refreshed successfully. memberId={}", memberId);

        return new LoginResult(
                newAccessToken,
                newRefreshToken,
                this.jwtProperties.getAccessTokenExpiration() / 1000,
                this.jwtProperties.getRefreshTokenExpiration() / 1000,
                member
        );
    }

    /**
     * Refresh Token의 유효성을 검증한다.
     *
     * @param refreshToken Refresh Token 값
     * @return 유효성 검증 결과
     */
    public TokenValidationResponse validateRefreshToken(String refreshToken) {
        // JWT 유효성 검증
        if (!this.jwtTokenProvider.validateToken(refreshToken)) {
            return TokenValidationResponse.invalid("유효하지 않은 토큰 형식입니다.");
        }

        // DB에서 토큰 조회
        return this.refreshTokenRepository.findByToken(refreshToken)
                .map(storedToken -> {
                    if (storedToken.isExpired()) {
                        return TokenValidationResponse.expired();
                    }
                    long secondsRemaining = ChronoUnit.SECONDS.between(
                            LocalDateTime.now(),
                            storedToken.getExpiresAt()
                    );
                    return TokenValidationResponse.valid(secondsRemaining);
                })
                .orElse(TokenValidationResponse.invalid("토큰을 찾을 수 없습니다."));
    }

    /**
     * 로그아웃한다.
     *
     * <p>Refresh Token을 DB에서 삭제하고, Access Token을 Redis 블랙리스트에 등록한다.</p>
     *
     * <p>accessToken이 null인 경우(쿠키 없이 호출된 경우 등)에도 Refresh Token은 정상 삭제되며
     * NPE 없이 정상 종료된다. 블랙리스트 등록은 건너뛴다.</p>
     *
     * <p>이미 만료된 accessToken이 전달된 경우에도 {@link com.dirtypay.domain.auth.security.jwt.JwtTokenProvider#extractJtiSafely(String)}를
     * 통해 jti를 추출하므로 {@link io.jsonwebtoken.ExpiredJwtException}이 발생하지 않는다.
     * 만료된 토큰의 잔여 유효 시간은 0으로 처리된다.</p>
     *
     * @param memberId    로그아웃할 회원 ID
     * @param accessToken 현재 Access Token 문자열 (jti 추출 용도), null 허용, 만료 토큰 허용
     */
    @Transactional
    public void logout(Long memberId, String accessToken) {
        this.refreshTokenRepository.deleteAllByMemberId(memberId);

        if (accessToken == null) {
            log.info("Member logged out without accessToken. memberId={}", memberId);
            return;
        }

        String jti = this.jwtTokenProvider.extractJtiSafely(accessToken);
        if (jti != null) {
            long remainingMillis = this.jwtTokenProvider.getRemainingExpiryMillis(accessToken);
            this.tokenBlacklistService.blacklistAccessToken(jti, remainingMillis);
        }

        log.info("Member logged out. memberId={}, jti={}", memberId,
                 jti != null ? jti : "null(legacy token)");
    }

    /**
     * 특정 Refresh Token으로 로그아웃한다.
     *
     * @param refreshToken Refresh Token 값
     */
    @Transactional
    public void logoutByToken(String refreshToken) {
        this.refreshTokenRepository.deleteByToken(refreshToken);
        log.info("Logged out by token successfully.");
    }

    /**
     * 로그인 결과를 담는 내부 클래스.
     *
     * @param accessToken            Access Token
     * @param refreshToken           Refresh Token
     * @param accessTokenExpiresIn   Access Token 만료 시간 (초)
     * @param refreshTokenExpiresIn  Refresh Token 만료 시간 (초)
     * @param member                 로그인한 회원 엔티티
     */
    public record LoginResult(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn,
            long refreshTokenExpiresIn,
            Member member
    ) {
    }
}
