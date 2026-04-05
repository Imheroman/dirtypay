package com.dirtypay.domain.group.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 그룹 멤버 응답 DTO.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class GroupMemberResponse {

    private Long orgMemberId;
    private String nickname;
    private boolean isCurrentUser;
    private List<PersonalOrderResponse> personalOrders;
    private BigDecimal totalAmount;
}
