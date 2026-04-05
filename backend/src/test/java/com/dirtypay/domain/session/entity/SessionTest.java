package com.dirtypay.domain.session.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    @Test
    @DisplayName("Session 생성 시 기본 status는 ACTIVE이다")
    void createSession_defaultStatusIsActive() {
        // given & when
        Session session = Session.builder()
                .title("테스트 세션")
                .ownerId(1L)
                .build();

        // then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Session 생성 시 모든 필드가 올바르게 설정된다")
    void createSession_allFieldsSet() {
        // given
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);

        // when
        Session session = Session.builder()
                .title("정산 세션")
                .description("2월 정산")
                .startDate(startDate)
                .endDate(endDate)
                .status(SessionStatus.ACTIVE)
                .ownerId(1L)
                .build();

        // then
        assertThat(session.getTitle()).isEqualTo("정산 세션");
        assertThat(session.getDescription()).isEqualTo("2월 정산");
        assertThat(session.getStartDate()).isEqualTo(startDate);
        assertThat(session.getEndDate()).isEqualTo(endDate);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getOwnerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Session 생성 시 startDate, endDate는 null일 수 있다")
    void createSession_nullableDateFields() {
        // given & when
        Session session = Session.builder()
                .title("날짜 없는 세션")
                .ownerId(1L)
                .build();

        // then
        assertThat(session.getStartDate()).isNull();
        assertThat(session.getEndDate()).isNull();
    }

    @Test
    @DisplayName("update() 호출 시 title, description, startDate, endDate가 변경된다")
    void update_success() {
        // given
        Session session = Session.builder()
                .title("원래 제목")
                .description("원래 설명")
                .ownerId(1L)
                .build();

        LocalDate newStart = LocalDate.of(2026, 3, 1);
        LocalDate newEnd = LocalDate.of(2026, 3, 31);

        // when
        session.update("새 제목", "새 설명", newStart, newEnd);

        // then
        assertThat(session.getTitle()).isEqualTo("새 제목");
        assertThat(session.getDescription()).isEqualTo("새 설명");
        assertThat(session.getStartDate()).isEqualTo(newStart);
        assertThat(session.getEndDate()).isEqualTo(newEnd);
    }

    @Test
    @DisplayName("archive() 호출 시 status가 ARCHIVED로 변경된다")
    void archive_success() {
        // given
        Session session = Session.builder()
                .title("활성 세션")
                .ownerId(1L)
                .build();

        // when
        session.archive();

        // then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ARCHIVED);
        assertThat(session.isActive()).isFalse();
    }

    @Test
    @DisplayName("isActive()는 ACTIVE 상태일 때 true를 반환한다")
    void isActive_returnsTrue() {
        // given
        Session session = Session.builder()
                .title("활성 세션")
                .ownerId(1L)
                .build();

        // then
        assertThat(session.isActive()).isTrue();
    }
}
