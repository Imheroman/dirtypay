package com.dirtypay.domain.group.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RoundGroupMember} 단위 테스트.
 *
 * <p>Builder 생성, 필드 getter, Soft Delete 동작을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class RoundGroupMemberTest {

    @Test
    @DisplayName("Builder로 생성 시 groupId, orgMemberId가 올바르게 설정된다")
    void createRoundGroupMember_allFieldsSet() {
        // given & when
        RoundGroupMember member = RoundGroupMember.builder()
                .groupId(10L)
                .orgMemberId(100L)
                .build();

        // then
        assertThat(member.getGroupId()).isEqualTo(10L);
        assertThat(member.getOrgMemberId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("delete() 호출 시 isDeleted()가 true로 변경된다 (BaseEntity Soft Delete)")
    void delete_softDeletesTheMember() {
        // given
        RoundGroupMember member = RoundGroupMember.builder()
                .groupId(10L)
                .orgMemberId(100L)
                .build();

        assertThat(member.isDeleted()).isFalse();

        // when
        member.delete();

        // then
        assertThat(member.isDeleted()).isTrue();
        assertThat(member.getDeletedDate()).isNotNull();
    }
}
