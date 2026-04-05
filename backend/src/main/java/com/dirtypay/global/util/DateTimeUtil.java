package com.dirtypay.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 날짜/시간 포맷팅 유틸리티.
 *
 * <p>일관된 날짜/시간 포맷팅을 제공하며, null 안전 처리를 보장한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public final class DateTimeUtil {

    /** 날짜+시간 포맷 패턴: {@code yyyy-MM-dd HH:mm:ss} */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 날짜 포맷 패턴: {@code yyyy-MM-dd} */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_PATTERN);

    private DateTimeUtil() {
        throw new AssertionError("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    /**
     * {@link LocalDateTime}을 {@code yyyy-MM-dd HH:mm:ss} 형식 문자열로 변환한다.
     *
     * @param dateTime 변환할 날짜+시간 (null 허용)
     * @return 포맷된 문자열, 입력이 null이면 null 반환
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * {@link LocalDate}를 {@code yyyy-MM-dd} 형식 문자열로 변환한다.
     *
     * @param date 변환할 날짜 (null 허용)
     * @return 포맷된 문자열, 입력이 null이면 null 반환
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }
}
