package com.dirtypay.domain.group.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoundGroupTest {

    @Test
    @DisplayName("루트 그룹 생성 시 모든 필드가 올바르게 설정된다")
    void createRootGroup_allFieldsSet() {
        // given & when
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .parentGroupId(null)
                .name("전체")
                .depth(0)
                .build();

        // then
        assertThat(group.getRoundId()).isEqualTo(1L);
        assertThat(group.getParentGroupId()).isNull();
        assertThat(group.getName()).isEqualTo("전체");
        assertThat(group.getDepth()).isEqualTo(0);
    }

    @Test
    @DisplayName("하위 그룹 생성 시 parentGroupId와 depth가 설정된다")
    void createChildGroup_withParentAndDepth() {
        // given & when
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .parentGroupId(10L)
                .name("1팀")
                .depth(1)
                .build();

        // then
        assertThat(group.getParentGroupId()).isEqualTo(10L);
        assertThat(group.getDepth()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateName() 호출 시 그룹명이 변경된다")
    void updateName_success() {
        // given
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .name("원래 이름")
                .depth(0)
                .build();

        // when
        group.updateName("새 이름");

        // then
        assertThat(group.getName()).isEqualTo("새 이름");
    }

    @Test
    @DisplayName("delete() 호출 시 Soft Delete된다 (BaseEntity)")
    void delete_softDelete() {
        // given
        RoundGroup group = RoundGroup.builder()
                .roundId(1L)
                .name("삭제 대상")
                .depth(0)
                .build();

        assertThat(group.isDeleted()).isFalse();

        // when
        group.delete();

        // then
        assertThat(group.isDeleted()).isTrue();
    }
}
