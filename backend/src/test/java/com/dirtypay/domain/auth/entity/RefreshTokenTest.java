package com.dirtypay.domain.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    @DisplayName("RefreshToken 생성 시 모든 필드가 올바르게 설정된다")
    void createRefreshToken_allFieldsSet() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 3, 1, 12, 0);

        // when
        RefreshToken token = RefreshToken.builder()
                .memberId(1L)
                .token("test-refresh-token")
                .expiresAt(expiresAt)
                .build();

        // then
        assertThat(token.getMemberId()).isEqualTo(1L);
        assertThat(token.getToken()).isEqualTo("test-refresh-token");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("RefreshToken 생성 시 createdAt이 자동으로 설정된다")
    void createRefreshToken_createdAtIsSet() {
        // given
        LocalDateTime before = LocalDateTime.now();

        // when
        RefreshToken token = RefreshToken.builder()
                .memberId(1L)
                .token("test-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // then
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.getCreatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("isExpired()는 만료 전이면 false를 반환한다")
    void isExpired_beforeExpiry_returnsFalse() {
        // given
        RefreshToken token = RefreshToken.builder()
                .memberId(1L)
                .token("valid-token")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        // then
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("isExpired()는 만료 후이면 true를 반환한다")
    void isExpired_afterExpiry_returnsTrue() {
        // given
        RefreshToken token = RefreshToken.builder()
                .memberId(1L)
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        // then
        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("updateToken() 호출 시 token과 expiresAt이 변경된다")
    void updateToken_changesTokenAndExpiresAt() {
        // given
        RefreshToken token = RefreshToken.builder()
                .memberId(1L)
                .token("old-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        LocalDateTime newExpiresAt = LocalDateTime.of(2026, 6, 1, 12, 0);

        // when
        token.updateToken("new-token", newExpiresAt);

        // then
        assertThat(token.getToken()).isEqualTo("new-token");
        assertThat(token.getExpiresAt()).isEqualTo(newExpiresAt);
        assertThat(token.getCreatedAt()).isNotNull();
    }
}
