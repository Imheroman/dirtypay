package com.dirtypay.domain.auth.security.blacklist;

import com.dirtypay.domain.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link RedisBlacklistCheckService} 단위 테스트.
 *
 * <p>TokenBlacklistService에 대한 위임 패턴을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisBlacklistCheckService 단위 테스트")
class RedisBlacklistCheckServiceTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private RedisBlacklistCheckService redisBlacklistCheckService;

    @Nested
    @DisplayName("isBlacklisted 위임 테스트")
    class IsBlacklistedTest {

        @Test
        @DisplayName("등록된 jti이면 TokenBlacklistService에 위임하여 true를 반환한다")
        void isBlacklisted_등록된jti_true반환() {
            // given
            String jti = "jti-1";
            given(tokenBlacklistService.isBlacklisted(jti)).willReturn(true);

            // when
            boolean result = redisBlacklistCheckService.isBlacklisted(jti);

            // then
            assertThat(result).isTrue();
            verify(tokenBlacklistService).isBlacklisted(jti);
        }

        @Test
        @DisplayName("미등록 jti이면 TokenBlacklistService에 위임하여 false를 반환한다")
        void isBlacklisted_미등록jti_false반환() {
            // given
            String jti = "jti-2";
            given(tokenBlacklistService.isBlacklisted(jti)).willReturn(false);

            // when
            boolean result = redisBlacklistCheckService.isBlacklisted(jti);

            // then
            assertThat(result).isFalse();
            verify(tokenBlacklistService).isBlacklisted(jti);
        }
    }
}
