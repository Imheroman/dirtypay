package com.dirtypay.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MemberRole} 단위 테스트.
 *
 * <p>각 권한의 authority 문자열, enum 값 목록 크기를 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class MemberRoleTest {

    @Test
    @DisplayName("ADMIN.getAuthority()는 'ROLE_ADMIN'을 반환한다")
    void adminAuthority_returnsRoleAdmin() {
        // when
        String authority = MemberRole.ADMIN.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("USER.getAuthority()는 'ROLE_USER'를 반환한다")
    void userAuthority_returnsRoleUser() {
        // when
        String authority = MemberRole.USER.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("MemberRole.values()는 정확히 2개의 권한을 포함한다")
    void values_hasTwoRoles() {
        // when
        MemberRole[] roles = MemberRole.values();

        // then
        assertThat(roles).hasSize(2);
        assertThat(roles).containsExactlyInAnyOrder(MemberRole.ADMIN, MemberRole.USER);
    }
}
