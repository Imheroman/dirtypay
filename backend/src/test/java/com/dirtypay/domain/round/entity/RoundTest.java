package com.dirtypay.domain.round.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RoundTest {

    @Test
    @DisplayName("Round 생성 시 status를 지정하지 않으면 기본값은 OPEN이다")
    void createRound_defaultStatusIsOpen() {
        // given & when
        Round round = Round.builder()
                .sessionId(1L)
                .title("1차 회식")
                .sortOrder(0)
                .build();

        // then
        assertThat(round.getStatus()).isEqualTo(RoundStatus.OPEN);
    }

    @Test
    @DisplayName("Round 생성 시 모든 필드가 올바르게 설정된다")
    void createRound_allFieldsSet() {
        // given
        LocalDate roundDate = LocalDate.of(2026, 3, 1);

        // when
        Round round = Round.builder()
                .sessionId(1L)
                .title("1차 회식")
                .place("강남역")
                .roundDate(roundDate)
                .status(RoundStatus.OPEN)
                .sortOrder(1)
                .build();

        // then
        assertThat(round.getSessionId()).isEqualTo(1L);
        assertThat(round.getTitle()).isEqualTo("1차 회식");
        assertThat(round.getPlace()).isEqualTo("강남역");
        assertThat(round.getRoundDate()).isEqualTo(roundDate);
        assertThat(round.getStatus()).isEqualTo(RoundStatus.OPEN);
        assertThat(round.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("update() 호출 시 title, place, roundDate, sortOrder가 변경된다")
    void update_success() {
        // given
        Round round = Round.builder()
                .sessionId(1L)
                .title("원래 제목")
                .place("원래 장소")
                .sortOrder(0)
                .build();

        LocalDate newDate = LocalDate.of(2026, 4, 1);

        // when
        round.update("새 제목", "새 장소", newDate, 2, null);

        // then
        assertThat(round.getTitle()).isEqualTo("새 제목");
        assertThat(round.getPlace()).isEqualTo("새 장소");
        assertThat(round.getRoundDate()).isEqualTo(newDate);
        assertThat(round.getSortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("changeStatus()로 CLOSED 상태로 변경할 수 있다")
    void changeStatus_toClosed() {
        // given
        Round round = Round.builder()
                .sessionId(1L)
                .title("라운드")
                .sortOrder(0)
                .build();

        // when
        round.changeStatus(RoundStatus.CLOSED);

        // then
        assertThat(round.getStatus()).isEqualTo(RoundStatus.CLOSED);
    }

    @Test
    @DisplayName("isOpen()은 OPEN 상태일 때 true를 반환한다")
    void isOpen_returnsTrue() {
        // given
        Round round = Round.builder()
                .sessionId(1L)
                .title("라운드")
                .sortOrder(0)
                .build();

        // then
        assertThat(round.isOpen()).isTrue();
        assertThat(round.isClosed()).isFalse();
    }

    @Test
    @DisplayName("isClosed()는 CLOSED 상태일 때 true를 반환한다")
    void isClosed_returnsTrue() {
        // given
        Round round = Round.builder()
                .sessionId(1L)
                .title("라운드")
                .status(RoundStatus.CLOSED)
                .sortOrder(0)
                .build();

        // then
        assertThat(round.isClosed()).isTrue();
        assertThat(round.isOpen()).isFalse();
    }

    @Test
    @DisplayName("changeStatus()로 CLOSED에서 OPEN으로 복원할 수 있다")
    void changeStatus_closedToOpen() {
        // given
        Round round = Round.builder()
                .sessionId(1L)
                .title("라운드")
                .status(RoundStatus.CLOSED)
                .sortOrder(0)
                .build();

        // when
        round.changeStatus(RoundStatus.OPEN);

        // then
        assertThat(round.isOpen()).isTrue();
    }
}
