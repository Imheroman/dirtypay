package com.dirtypay.domain.auth.security;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.domain.member.repository.MemberRepository;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * CustomUserDetailsService 단위 테스트.
 *
 * <p>{@link CustomUserDetailsService#loadUserByUsername(String)} 메서드가
 * 정상 조회, 미존재 이메일 예외, USER/ADMIN 권한 변환을 올바르게 처리하는지 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 단위 테스트")
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("loadUserByUsername 메서드 테스트")
    class LoadUserByUsernameTest {

        @Test
        @DisplayName("존재하는 이메일로 조회 시 UserPrincipal 반환")
        void loadUserByUsername_success_returnsUserPrincipal() {
            // given
            String email = "user@test.com";
            Member member = createMember(1L, email, "테스트유저", MemberRole.USER);

            given(memberRepository.findByEmail(email))
                    .willReturn(Optional.of(member));

            // when
            UserDetails result = customUserDetailsService.loadUserByUsername(email);

            // then
            assertThat(result).isInstanceOf(UserPrincipal.class);

            UserPrincipal principal = (UserPrincipal) result;
            assertThat(principal.getUsername()).isEqualTo(email);
            assertThat(principal.getId()).isEqualTo(1L);
            assertThat(principal.getName()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회 시 EntityNotFoundException 발생")
        void loadUserByUsername_notFound_throwsEntityNotFoundException() {
            // given
            String notExistEmail = "notfound@test.com";

            given(memberRepository.findByEmail(notExistEmail))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(notExistEmail))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("USER 권한을 가진 회원 조회 시 ROLE_USER 권한 부여")
        void loadUserByUsername_userRole_hasRoleUser() {
            // given
            String email = "user@test.com";
            Member member = createMember(1L, email, "일반유저", MemberRole.USER);

            given(memberRepository.findByEmail(email))
                    .willReturn(Optional.of(member));

            // when
            UserDetails result = customUserDetailsService.loadUserByUsername(email);

            // then
            // UserPrincipal.authorities 에 ROLE_USER 권한이 포함되어야 한다
            assertThat(result.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("ADMIN 권한을 가진 회원 조회 시 ROLE_ADMIN 권한 부여")
        void loadUserByUsername_adminRole_hasRoleAdmin() {
            // given
            String email = "admin@test.com";
            Member member = createMember(2L, email, "관리자", MemberRole.ADMIN);

            given(memberRepository.findByEmail(email))
                    .willReturn(Optional.of(member));

            // when
            UserDetails result = customUserDetailsService.loadUserByUsername(email);

            // then
            // UserPrincipal.authorities 에 ROLE_ADMIN 권한이 포함되어야 한다
            assertThat(result.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }
    }

    /**
     * 테스트용 Member 객체를 생성한다.
     *
     * @param id    회원 ID (ReflectionTestUtils로 주입)
     * @param email 이메일
     * @param name  이름
     * @param role  회원 권한
     * @return 생성된 Member 객체
     */
    private Member createMember(Long id, String email, String name, MemberRole role) {
        Member member = Member.builder()
                .email(email)
                .password("encodedPassword")
                .name(name)
                .role(role)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
