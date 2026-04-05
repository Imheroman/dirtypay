package com.dirtypay.domain.joinrequest.entity;

/**
 * 참여 요청 상태.
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public enum JoinRequestStatus {

    /** 대기 중 */
    PENDING,

    /** 승인됨 */
    APPROVED,

    /** 거절됨 */
    REJECTED
}
