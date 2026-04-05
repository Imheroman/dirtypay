package com.dirtypay.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilTest {

    @Test
    @DisplayName("LocalDateTime 포맷: yyyy-MM-dd HH:mm:ss 형식 반환")
    void format_localDateTime() {
        // given
        LocalDateTime dateTime = LocalDateTime.of(2026, 2, 16, 14, 30, 45);

        // when
        String result = DateTimeUtil.format(dateTime);

        // then
        assertThat(result).isEqualTo("2026-02-16 14:30:45");
    }

    @Test
    @DisplayName("LocalDate 포맷: yyyy-MM-dd 형식 반환")
    void formatDate_localDate() {
        // given
        LocalDate date = LocalDate.of(2026, 2, 16);

        // when
        String result = DateTimeUtil.formatDate(date);

        // then
        assertThat(result).isEqualTo("2026-02-16");
    }

    @Test
    @DisplayName("자정(00:00:00) 시간 포맷 검증")
    void format_midnight() {
        // given
        LocalDateTime midnight = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        // when
        String result = DateTimeUtil.format(midnight);

        // then
        assertThat(result).isEqualTo("2026-01-01 00:00:00");
    }

    @Test
    @DisplayName("연말(12-31) 날짜 포맷 검증")
    void formatDate_yearEnd() {
        // given
        LocalDate yearEnd = LocalDate.of(2026, 12, 31);

        // when
        String result = DateTimeUtil.formatDate(yearEnd);

        // then
        assertThat(result).isEqualTo("2026-12-31");
    }

    @Test
    @DisplayName("null 입력 시 NPE 미발생, null 반환")
    void format_nullInput_returnsNull() {
        // when & then
        assertThat(DateTimeUtil.format(null)).isNull();
        assertThat(DateTimeUtil.formatDate(null)).isNull();
    }
}
