package com.dirtypay.domain.session.dto.response;

import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.entity.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 세션 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class SessionResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private SessionStatus status;
    private Long ownerId;
    private String inviteCode;
    private long memberCount;
    private long roundCount;
    private BigDecimal totalAmount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * Session 엔티티로부터 응답 DTO를 생성한다.
     *
     * <p>집계 필드는 기본값(0, ZERO)으로 설정된다.
     * 세션 생성/수정 시 사용한다.</p>
     *
     * @param session 세션 엔티티
     * @return 세션 응답 DTO
     */
    public static SessionResponse from(Session session) {
        return SessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .description(session.getDescription())
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .status(session.getStatus())
                .ownerId(session.getOwnerId())
                .inviteCode(session.getInviteCode())
                .memberCount(0)
                .roundCount(0)
                .totalAmount(BigDecimal.ZERO)
                .createdDate(session.getCreatedDate())
                .updatedDate(session.getUpdatedDate())
                .build();
    }

    /**
     * Session 엔티티와 집계 데이터로부터 응답 DTO를 생성한다.
     *
     * <p>세션 목록/상세 조회 시 참여 인원, 라운드 수, 총 금액을 포함하여 응답한다.</p>
     *
     * @param session     세션 엔티티
     * @param memberCount 참여 인원 수
     * @param roundCount  라운드 수
     * @param totalAmount 총 금액
     * @return 세션 응답 DTO
     */
    public static SessionResponse from(Session session, long memberCount, long roundCount,
                                        BigDecimal totalAmount) {
        return SessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .description(session.getDescription())
                .startDate(session.getStartDate())
                .endDate(session.getEndDate())
                .status(session.getStatus())
                .ownerId(session.getOwnerId())
                .inviteCode(session.getInviteCode())
                .memberCount(memberCount)
                .roundCount(roundCount)
                .totalAmount(totalAmount)
                .createdDate(session.getCreatedDate())
                .updatedDate(session.getUpdatedDate())
                .build();
    }
}
