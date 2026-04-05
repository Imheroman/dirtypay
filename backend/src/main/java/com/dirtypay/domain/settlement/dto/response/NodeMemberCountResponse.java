package com.dirtypay.domain.settlement.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 노드별 참여자 주문 횟수 요약 응답 DTO.
 *
 * <p>특정 메뉴에 대해 멤버가 몇 번 주문에 참여했는지를 나타낸다.</p>
 * <p>예시: "gg ×2"</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class NodeMemberCountResponse {

    /** 조직 멤버 ID. */
    private Long orgMemberId;

    /** 멤버 닉네임. */
    private String nickname;

    /** 해당 메뉴 주문 참여 횟수. */
    private int count;
}
