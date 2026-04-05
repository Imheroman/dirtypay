package com.dirtypay.domain.auth.security.blacklist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * {@link DbBlacklistCheckService} 단위 테스트.
 *
 * <p>JdbcTemplate Mock을 사용하여 DB 기반 블랙리스트 조회 동작을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DbBlacklistCheckService 단위 테스트")
class DbBlacklistCheckServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DbBlacklistCheckService dbBlacklistCheckService;

    @Nested
    @DisplayName("isBlacklisted 테스트")
    class IsBlacklistedTest {

        @Test
        @DisplayName("DB에 jti가 존재하면 true를 반환한다")
        void isBlacklisted_DB존재_true반환() {
            // given
            String jti = "test-jti";
            given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(jti)))
                    .willReturn(1);

            // when
            boolean result = dbBlacklistCheckService.isBlacklisted(jti);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("DB에 jti가 없으면 false를 반환한다")
        void isBlacklisted_DB부재_false반환() {
            // given
            String jti = "unknown-jti";
            given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(jti)))
                    .willReturn(0);

            // when
            boolean result = dbBlacklistCheckService.isBlacklisted(jti);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("queryForObject가 null을 반환하면 NullPointerException 없이 false를 반환한다")
        void isBlacklisted_queryForObjectNull_false반환() {
            // given
            // DbBlacklistCheckService.isBlacklisted()는 count != null && count > 0 으로
            // null 안전 처리가 되어 있으므로 NPE 없이 false를 반환한다.
            String jti = "any-jti";
            given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(jti)))
                    .willReturn(null);

            // when
            boolean result = dbBlacklistCheckService.isBlacklisted(jti);

            // then
            assertThat(result).isFalse();
        }
    }
}
