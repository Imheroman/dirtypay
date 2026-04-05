package com.dirtypay.global.lock;

import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;

/**
 * 분산 락 획득 실패 예외.
 *
 * <p>다른 요청이 같은 락을 보유 중일 때 대기 시간 초과 시 발생한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public class LockAcquisitionFailedException extends BusinessException {

    /**
     * 락 키 정보를 포함한 예외를 생성한다.
     *
     * @param lockKey 획득 실패한 락 키
     */
    public LockAcquisitionFailedException(String lockKey) {
        super(ErrorCode.LOCK_ACQUISITION_FAILED, "락 획득 실패: " + lockKey);
    }
}
