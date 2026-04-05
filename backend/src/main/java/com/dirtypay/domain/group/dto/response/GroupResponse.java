package com.dirtypay.domain.group.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 그룹 응답 DTO.
 *
 * <p>계층 구조를 지원하며 재귀적으로 하위 그룹을 포함한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@Builder
public class GroupResponse {

    private Long groupId;
    private String groupName;
    private Long parentGroupId;
    private int depth;
    private boolean isParticipating;

    @Builder.Default
    private List<SharedMenuResponse> sharedMenus = new ArrayList<>();

    @Builder.Default
    private List<GroupMemberResponse> members = new ArrayList<>();

    @Builder.Default
    private List<GroupResponse> childGroups = new ArrayList<>();

    @Setter
    private BigDecimal totalAmount;
}
