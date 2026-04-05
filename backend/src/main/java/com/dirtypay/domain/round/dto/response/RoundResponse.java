package com.dirtypay.domain.round.dto.response;

import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 라운드 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class RoundResponse {

    private Long id;
    private Long sessionId;
    private String title;
    private String place;
    private LocalDate roundDate;
    private RoundStatus status;
    private int sortOrder;
    private Long storeId;
    private BigDecimal totalAmount;
    private long participantCount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * Round 엔티티로부터 응답 DTO를 생성한다.
     *
     * <p>집계 필드는 기본값(ZERO, 0)으로 설정된다.
     * 라운드 생성/수정 시 사용한다.</p>
     *
     * @param round 라운드 엔티티
     * @return 라운드 응답 DTO
     */
    public static RoundResponse from(Round round) {
        return RoundResponse.builder()
                .id(round.getId())
                .sessionId(round.getSessionId())
                .title(round.getTitle())
                .place(round.getPlace())
                .roundDate(round.getRoundDate())
                .status(round.getStatus())
                .sortOrder(round.getSortOrder())
                .storeId(round.getStoreId())
                .totalAmount(BigDecimal.ZERO)
                .participantCount(0)
                .createdDate(round.getCreatedDate())
                .updatedDate(round.getUpdatedDate())
                .build();
    }

    /**
     * Round 엔티티와 집계 데이터로부터 응답 DTO를 생성한다.
     *
     * <p>라운드 목록/상세 조회 시 총 금액, 참여자 수를 포함하여 응답한다.</p>
     *
     * @param round            라운드 엔티티
     * @param totalAmount      총 금액
     * @param participantCount 참여자 수
     * @return 라운드 응답 DTO
     */
    public static RoundResponse from(Round round, BigDecimal totalAmount, long participantCount) {
        return RoundResponse.builder()
                .id(round.getId())
                .sessionId(round.getSessionId())
                .title(round.getTitle())
                .place(round.getPlace())
                .roundDate(round.getRoundDate())
                .status(round.getStatus())
                .sortOrder(round.getSortOrder())
                .storeId(round.getStoreId())
                .totalAmount(totalAmount)
                .participantCount(participantCount)
                .createdDate(round.getCreatedDate())
                .updatedDate(round.getUpdatedDate())
                .build();
    }
}
