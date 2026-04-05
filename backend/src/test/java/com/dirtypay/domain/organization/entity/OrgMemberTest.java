package com.dirtypay.domain.organization.entity;

import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrgMemberTest {

    @Test
    @DisplayName("OrgMember 생성 시 기본 isActive는 true이다")
    void createOrgMember_defaultIsActive() {
        // given & when
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("홍길동")
                .build();

        // then
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("OrgMember 생성 시 모든 필드가 올바르게 설정된다")
    void createOrgMember_allFieldsSet() {
        // given & when
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .userId(10L)
                .nickname("개발자")
                .isActive(true)
                .build();

        // then
        assertThat(member.getSessionId()).isEqualTo(1L);
        assertThat(member.getUserId()).isEqualTo(10L);
        assertThat(member.getNickname()).isEqualTo("개발자");
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("비회원 OrgMember는 userId가 null이다")
    void createOrgMember_guestUser() {
        // given & when
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("비회원참여자")
                .build();

        // then
        assertThat(member.getUserId()).isNull();
        assertThat(member.isLinkedUser()).isFalse();
        assertThat(member.getNickname()).isEqualTo("비회원참여자");
    }

    @Test
    @DisplayName("회원 연결된 OrgMember는 isLinkedUser가 true이다")
    void isLinkedUser_true() {
        // given & when
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .userId(10L)
                .nickname("회원")
                .build();

        // then
        assertThat(member.isLinkedUser()).isTrue();
    }

    @Test
    @DisplayName("linkUser() 호출 시 userId가 연결된다")
    void linkUser_success() {
        // given
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("비회원참여자")
                .build();

        // when
        member.linkUser(10L);

        // then
        assertThat(member.getUserId()).isEqualTo(10L);
        assertThat(member.isLinkedUser()).isTrue();
    }

    @Test
    @DisplayName("이미 회원이 연결된 OrgMember에 linkUser() 호출 시 BusinessException 발생")
    void linkUser_alreadyLinked_throwsException() {
        // given
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .userId(10L)
                .nickname("회원")
                .build();

        // when & then
        assertThatThrownBy(() -> member.linkUser(20L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_ALREADY_LINKED);
    }

    @Test
    @DisplayName("linkUser()에 null 전달 시 NullPointerException 발생")
    void linkUser_nullUserId_throwsException() {
        // given
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("비회원참여자")
                .build();

        // when & then
        assertThatThrownBy(() -> member.linkUser(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("update() 호출 시 nickname이 변경된다")
    void update_success() {
        // given
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("원래이름")
                .build();

        // when
        member.update("새이름", null);

        // then
        assertThat(member.getNickname()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("deactivate() 호출 시 isActive가 false가 된다")
    void deactivate_success() {
        // given
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("참여자")
                .build();

        // when
        member.deactivate();

        // then
        assertThat(member.isActive()).isFalse();
    }

    @Test
    @DisplayName("activate() 호출 시 isActive가 true가 된다")
    void activate_success() {
        // given
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("참여자")
                .isActive(false)
                .build();

        // when
        member.activate();

        // then
        assertThat(member.isActive()).isTrue();
    }
}
