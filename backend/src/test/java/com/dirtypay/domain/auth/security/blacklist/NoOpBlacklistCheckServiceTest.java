package com.dirtypay.domain.auth.security.blacklist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NoOpBlacklistCheckService} 단위 테스트.
 *
 * <p>항상 false를 반환하는 NoOp 구현체의 동작을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DisplayName("NoOpBlacklistCheckService 단위 테스트")
class NoOpBlacklistCheckServiceTest {

    private final NoOpBlacklistCheckService service = new NoOpBlacklistCheckService();

    @Nested
    @DisplayName("isBlacklisted 테스트")
    class IsBlacklistedTest {

        @Test
        @DisplayName("임의의 jti 입력 시 항상 false를 반환한다")
        void isBlacklisted_임의jti_false반환() {
            // given
            String jti = "any-jti";

            // when
            boolean result = service.isBlacklisted(jti);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 입력 시 NullPointerException 없이 false를 반환한다")
        void isBlacklisted_null입력_false반환() {
            // given & when
            boolean result = service.isBlacklisted(null);

            // then
            assertThat(result).isFalse();
        }
    }
}
