package com.dirtypay.domain.joinrequest.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 세션 참여 요청 엔티티.
 *
 * <p>사용자가 초대 코드를 통해 세션 참여를 요청하면 PENDING 상태로 생성되며,
 * 세션 소유자가 승인(APPROVED) 또는 거절(REJECTED)할 수 있다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "join_requests")
@SQLRestriction("deleted_date IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JoinRequest extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private String nickname;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinRequestStatus status;

    @Builder
    public JoinRequest(Long sessionId, Long requesterId, String nickname, String message) {
        this.sessionId = sessionId;
        this.requesterId = requesterId;
        this.nickname = nickname;
        this.message = message;
        this.status = JoinRequestStatus.PENDING;
    }

    /**
     * 참여 요청을 승인한다.
     *
     * @throws BusinessException PENDING 상태가 아닌 경우
     */
    public void approve() {
        this.verifyPending();
        this.status = JoinRequestStatus.APPROVED;
    }

    /**
     * 참여 요청을 거절한다.
     *
     * @throws BusinessException PENDING 상태가 아닌 경우
     */
    public void reject() {
        this.verifyPending();
        this.status = JoinRequestStatus.REJECTED;
    }

    /**
     * PENDING 상태인지 확인한다.
     *
     * @return PENDING 여부
     */
    public boolean isPending() {
        return this.status == JoinRequestStatus.PENDING;
    }

    private void verifyPending() {
        if (!this.isPending()) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_NOT_PENDING);
        }
    }
}
