package com.dirtypay.domain.auth.security;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserPrincipal 단위 테스트.
 *
 * <p>Member 엔티티로부터 UserPrincipal을 생성하고,
 * Spring Security UserDetails 계약을 올바르게 구현하는지 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class UserPrincipalTest {

    /**
     * 테스트용 Member 객체를 생성한다.
     *
     * @param id    회원 ID
     * @param email 이메일
     * @param role  권한
     * @return Member 인스턴스
     */
    private Member createMember(Long id, String email, MemberRole role) {
        Member member = Member.builder()
                .email(email)
                .password("encodedPassword")
                .name("테스트사용자")
                .role(role)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Nested
    @DisplayName("from(member) 변환 테스트")
    class FromMemberTest {

        @Test
        @DisplayName("Member로부터 UserPrincipal 변환 시 id가 올바르게 매핑된다")
        void from_idIsMapped() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);

            // when
            UserPrincipal principal = UserPrincipal.from(member);

            // then
            assertThat(principal.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Member로부터 UserPrincipal 변환 시 email이 올바르게 매핑된다")
        void from_emailIsMapped() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);

            // when
            UserPrincipal principal = UserPrincipal.from(member);

            // then
            assertThat(principal.getEmail()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("Member로부터 UserPrincipal 변환 시 password가 올바르게 매핑된다")
        void from_passwordIsMapped() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);

            // when
            UserPrincipal principal = UserPrincipal.from(member);

            // then
            assertThat(principal.getPassword()).isEqualTo("encodedPassword");
        }

        @Test
        @DisplayName("Member로부터 UserPrincipal 변환 시 name이 올바르게 매핑된다")
        void from_nameIsMapped() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);

            // when
            UserPrincipal principal = UserPrincipal.from(member);

            // then
            assertThat(principal.getName()).isEqualTo("테스트사용자");
        }

        @Test
        @DisplayName("USER 권한 Member 변환 시 ROLE_USER GrantedAuthority가 부여된다")
        void from_userRole_hasRoleUserAuthority() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);

            // when
            UserPrincipal principal = UserPrincipal.from(member);

            // then
            Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
            assertThat(authorities).hasSize(1);
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("ADMIN 권한 Member 변환 시 ROLE_ADMIN GrantedAuthority가 부여된다")
        void from_adminRole_hasRoleAdminAuthority() {
            // given
            Member member = createMember(2L, "admin@test.com", MemberRole.ADMIN);

            // when
            UserPrincipal principal = UserPrincipal.from(member);

            // then
            Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
            assertThat(authorities).hasSize(1);
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("getUsername 테스트")
    class GetUsernameTest {

        @Test
        @DisplayName("getUsername은 email을 반환한다")
        void getUsername_returnsEmail() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);
            UserPrincipal principal = UserPrincipal.from(member);

            // when
            String username = principal.getUsername();

            // then
            assertThat(username).isEqualTo(member.getEmail());
        }
    }

    @Nested
    @DisplayName("UserDetails 계약 테스트")
    class UserDetailsContractTest {

        @Test
        @DisplayName("isAccountNonExpired는 항상 true를 반환한다")
        void isAccountNonExpired_alwaysTrue() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);
            UserPrincipal principal = UserPrincipal.from(member);

            // when & then
            assertThat(principal.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("isAccountNonLocked는 항상 true를 반환한다")
        void isAccountNonLocked_alwaysTrue() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);
            UserPrincipal principal = UserPrincipal.from(member);

            // when & then
            assertThat(principal.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("isCredentialsNonExpired는 항상 true를 반환한다")
        void isCredentialsNonExpired_alwaysTrue() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);
            UserPrincipal principal = UserPrincipal.from(member);

            // when & then
            assertThat(principal.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("isEnabled는 항상 true를 반환한다")
        void isEnabled_alwaysTrue() {
            // given
            Member member = createMember(1L, "user@test.com", MemberRole.USER);
            UserPrincipal principal = UserPrincipal.from(member);

            // when & then
            assertThat(principal.isEnabled()).isTrue();
        }
    }
}
