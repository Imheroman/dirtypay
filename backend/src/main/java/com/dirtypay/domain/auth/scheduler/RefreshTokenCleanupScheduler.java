package com.dirtypay.domain.auth.scheduler;

import com.dirtypay.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 Refresh Token을 주기적으로 정리하는 스케줄러.
 *
 * <p>매일 새벽 3시에 실행되어 만료된 Refresh Token을 DB에서 삭제한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 만료된 Refresh Token을 삭제한다.
     *
     * <p>매일 새벽 3시에 실행되며, 현재 시간 기준으로 만료된 토큰을 일괄 삭제한다.</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanUpExpiredTokens() {
        int deletedCount = this.refreshTokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
        if (deletedCount > 0) {
            log.info("Expired refresh tokens cleaned up: count={}", deletedCount);
        }
    }
}
