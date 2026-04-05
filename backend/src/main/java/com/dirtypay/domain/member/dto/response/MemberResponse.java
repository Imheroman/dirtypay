package com.dirtypay.domain.member.dto.response;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 회원 정보 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class MemberResponse {

    private Long id;
    private String email;
    private String name;
    private String profileImage;
    private MemberRole role;
    private LocalDateTime createdDate;

    /**
     * Member 엔티티로부터 응답 DTO를 생성한다.
     *
     * @param member 회원 엔티티
     * @return MemberResponse 인스턴스
     */
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .profileImage(member.getProfileImage())
                .role(member.getRole())
                .createdDate(member.getCreatedDate())
                .build();
    }
}
