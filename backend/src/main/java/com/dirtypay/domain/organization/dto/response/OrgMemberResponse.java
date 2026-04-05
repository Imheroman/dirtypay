package com.dirtypay.domain.organization.dto.response;

import com.dirtypay.domain.organization.entity.OrgMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 조직도 멤버 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class OrgMemberResponse {

    private Long id;
    private Long sessionId;
    private Long userId;
    private String nickname;
    private boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    /**
     * OrgMember 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param member 조직도 멤버 엔티티
     * @return 멤버 응답 DTO
     */
    public static OrgMemberResponse from(OrgMember member) {
        return OrgMemberResponse.builder()
                .id(member.getId())
                .sessionId(member.getSessionId())
                .userId(member.getUserId())
                .nickname(member.getNickname())
                .isActive(member.isActive())
                .createdDate(member.getCreatedDate())
                .updatedDate(member.getUpdatedDate())
                .build();
    }
}
