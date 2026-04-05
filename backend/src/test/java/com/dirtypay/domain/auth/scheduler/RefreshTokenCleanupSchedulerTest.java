package com.dirtypay.domain.auth.scheduler;

import com.dirtypay.domain.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link RefreshTokenCleanupScheduler} 단위 테스트.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @InjectMocks
    private RefreshTokenCleanupScheduler scheduler;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("만료된 토큰이 있으면 삭제한다")
    void cleanUpExpiredTokens_deletesExpiredTokens() {
        // given
        given(refreshTokenRepository.deleteAllExpiredTokens(any(LocalDateTime.class)))
                .willReturn(5);

        // when
        scheduler.cleanUpExpiredTokens();

        // then
        then(refreshTokenRepository).should().deleteAllExpiredTokens(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("만료된 토큰이 없으면 삭제 건수가 0이다")
    void cleanUpExpiredTokens_noExpiredTokens() {
        // given
        given(refreshTokenRepository.deleteAllExpiredTokens(any(LocalDateTime.class)))
                .willReturn(0);

        // when
        scheduler.cleanUpExpiredTokens();

        // then
        then(refreshTokenRepository).should().deleteAllExpiredTokens(any(LocalDateTime.class));
    }
}
