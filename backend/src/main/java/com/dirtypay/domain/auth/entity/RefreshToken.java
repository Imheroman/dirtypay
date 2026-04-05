package com.dirtypay.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh Token 저장을 위한 Entity.
 *
 * <p>Refresh Token을 DB에 저장하여 다음 기능을 지원한다:</p>
 * <ul>
 *   <li>토큰 유효성 검증 (저장된 토큰과 비교)</li>
 *   <li>로그아웃 시 토큰 폐기</li>
 *   <li>강제 로그아웃 (관리자 기능)</li>
 * </ul>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token_member_id", columnList = "member_id"),
        @Index(name = "idx_refresh_token_token", columnList = "token", unique = true)
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 토큰을 소유한 회원의 ID.
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * Refresh Token 값.
     * JWT 형식의 토큰 문자열을 저장한다.
     */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /**
     * 토큰 만료 시간.
     * 이 시간이 지나면 토큰은 더 이상 유효하지 않다.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 토큰 생성 시간.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * RefreshToken 생성자.
     *
     * @param memberId  회원 ID
     * @param token     Refresh Token 값
     * @param expiresAt 만료 시간
     */
    @Builder
    public RefreshToken(Long memberId, String token, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 토큰이 만료되었는지 확인한다.
     *
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 토큰을 새 토큰으로 갱신한다.
     *
     * @param newToken     새 Refresh Token 값
     * @param newExpiresAt 새 만료 시간
     */
    public void updateToken(String newToken, LocalDateTime newExpiresAt) {
        this.token = newToken;
        this.expiresAt = newExpiresAt;
        this.createdAt = LocalDateTime.now();
    }
}
