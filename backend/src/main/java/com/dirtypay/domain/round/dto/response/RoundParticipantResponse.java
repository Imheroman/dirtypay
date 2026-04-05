package com.dirtypay.domain.round.dto.response;

import com.dirtypay.domain.round.entity.RoundParticipant;
import lombok.Builder;
import lombok.Getter;

/**
 * 라운드 참여자 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class RoundParticipantResponse {

    private Long id;
    private Long roundId;
    private Long orgMemberId;
    private String nickname;
    private boolean isExcluded;

    /**
     * RoundParticipant 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param participant 참여자 엔티티
     * @param nickname    참여자 닉네임
     * @return 참여자 응답 DTO
     */
    public static RoundParticipantResponse from(RoundParticipant participant, String nickname) {
        return RoundParticipantResponse.builder()
                .id(participant.getId())
                .roundId(participant.getRoundId())
                .orgMemberId(participant.getOrgMemberId())
                .nickname(nickname)
                .isExcluded(participant.isExcluded())
                .build();
    }
}
