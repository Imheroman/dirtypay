package com.dirtypay.domain.member.dto.response;

import com.dirtypay.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 검색 응답 DTO.
 *
 * <p>세션 멤버 추가 등 검색 용도에 필요한 최소 정보만 포함한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class MemberSearchResponse {

    private Long id;
    private String email;
    private String name;
    private String profileImage;

    /**
     * Member 엔티티로부터 검색 응답 DTO를 생성한다.
     *
     * @param member 회원 엔티티
     * @return MemberSearchResponse 인스턴스
     */
    public static MemberSearchResponse from(Member member) {
        return MemberSearchResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .profileImage(member.getProfileImage())
                .build();
    }
}
