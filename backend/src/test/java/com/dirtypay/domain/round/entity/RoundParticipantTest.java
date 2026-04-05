package com.dirtypay.domain.round.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoundParticipantTest {

    @Test
    @DisplayName("RoundParticipant 생성 시 기본 isExcluded는 false이다")
    void createRoundParticipant_defaultExcludedIsFalse() {
        // given & when
        RoundParticipant participant = RoundParticipant.builder()
                .roundId(1L)
                .orgMemberId(10L)
                .build();

        // then
        assertThat(participant.getRoundId()).isEqualTo(1L);
        assertThat(participant.getOrgMemberId()).isEqualTo(10L);
        assertThat(participant.isExcluded()).isFalse();
    }

    @Test
    @DisplayName("RoundParticipant 생성 시 isExcluded를 true로 설정할 수 있다")
    void createRoundParticipant_excludedTrue() {
        // given & when
        RoundParticipant participant = RoundParticipant.builder()
                .roundId(1L)
                .orgMemberId(10L)
                .isExcluded(true)
                .build();

        // then
        assertThat(participant.isExcluded()).isTrue();
    }

    @Test
    @DisplayName("exclude() 호출 시 isExcluded가 true로 변경된다")
    void exclude_setsExcludedTrue() {
        // given
        RoundParticipant participant = RoundParticipant.builder()
                .roundId(1L)
                .orgMemberId(10L)
                .build();

        // when
        participant.exclude();

        // then
        assertThat(participant.isExcluded()).isTrue();
    }

    @Test
    @DisplayName("include() 호출 시 isExcluded가 false로 변경된다")
    void include_setsExcludedFalse() {
        // given
        RoundParticipant participant = RoundParticipant.builder()
                .roundId(1L)
                .orgMemberId(10L)
                .isExcluded(true)
                .build();

        // when
        participant.include();

        // then
        assertThat(participant.isExcluded()).isFalse();
    }

    @Test
    @DisplayName("exclude() 후 include() 호출하면 false로 복구된다")
    void excludeThenInclude_restoresToFalse() {
        // given
        RoundParticipant participant = RoundParticipant.builder()
                .roundId(1L)
                .orgMemberId(10L)
                .build();

        // when
        participant.exclude();
        assertThat(participant.isExcluded()).isTrue();

        participant.include();

        // then
        assertThat(participant.isExcluded()).isFalse();
    }
}
