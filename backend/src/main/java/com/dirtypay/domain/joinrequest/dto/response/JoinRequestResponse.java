package com.dirtypay.domain.joinrequest.dto.response;

import com.dirtypay.domain.joinrequest.entity.JoinRequest;
import com.dirtypay.domain.joinrequest.entity.JoinRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 참여 요청 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class JoinRequestResponse {

    private Long id;
    private Long sessionId;
    private Long requesterId;
    private String nickname;
    private String message;
    private JoinRequestStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * JoinRequest 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param joinRequest 참여 요청 엔티티
     * @return 참여 요청 응답 DTO
     */
    public static JoinRequestResponse from(JoinRequest joinRequest) {
        return JoinRequestResponse.builder()
                .id(joinRequest.getId())
                .sessionId(joinRequest.getSessionId())
                .requesterId(joinRequest.getRequesterId())
                .nickname(joinRequest.getNickname())
                .message(joinRequest.getMessage())
                .status(joinRequest.getStatus())
                .createdDate(joinRequest.getCreatedDate())
                .updatedDate(joinRequest.getUpdatedDate())
                .build();
    }
}
