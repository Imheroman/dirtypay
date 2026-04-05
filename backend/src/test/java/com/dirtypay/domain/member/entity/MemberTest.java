package com.dirtypay.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    @DisplayName("Member 생성 시 기본 role은 USER이다")
    void createMember_defaultRoleIsUser() {
        // given & when
        Member member = Member.builder()
                .email("test@test.com")
                .password("password123")
                .name("테스트")
                .build();

        // then
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    @DisplayName("Member 생성 시 role을 지정할 수 있다")
    void createMember_withSpecifiedRole() {
        // given & when
        Member member = Member.builder()
                .email("admin@test.com")
                .password("password123")
                .name("관리자")
                .role(MemberRole.ADMIN)
                .build();

        // then
        assertThat(member.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    @DisplayName("프로필 업데이트가 정상 동작한다")
    void updateProfile_success() {
        // given
        Member member = Member.builder()
                .email("test@test.com")
                .password("password123")
                .name("원래이름")
                .profileImage("old-image.jpg")
                .build();

        // when
        member.updateProfile("새이름", "new-image.jpg");

        // then
        assertThat(member.getName()).isEqualTo("새이름");
        assertThat(member.getProfileImage()).isEqualTo("new-image.jpg");
    }

    @Test
    @DisplayName("비밀번호 변경이 정상 동작한다")
    void changePassword_success() {
        // given
        Member member = Member.builder()
                .email("test@test.com")
                .password("oldPassword")
                .name("테스트")
                .build();

        // when
        member.changePassword("newPassword");

        // then
        assertThat(member.getPassword()).isEqualTo("newPassword");
    }
}
