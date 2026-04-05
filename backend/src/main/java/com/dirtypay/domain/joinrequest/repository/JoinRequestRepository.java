package com.dirtypay.domain.joinrequest.repository;

import com.dirtypay.domain.joinrequest.entity.JoinRequest;
import com.dirtypay.domain.joinrequest.entity.JoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 참여 요청 리포지토리.
 *
 * <p>JoinRequest 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    /**
     * 세션 ID와 요청자 ID, 상태로 참여 요청을 조회한다.
     *
     * @param sessionId   세션 ID
     * @param requesterId 요청자 ID
     * @param status      요청 상태
     * @return 참여 요청 Optional
     */
    Optional<JoinRequest> findBySessionIdAndRequesterIdAndStatus(
            Long sessionId, Long requesterId, JoinRequestStatus status);

    /**
     * 세션 ID로 참여 요청 목록을 페이지 단위로 조회한다.
     *
     * @param sessionId 세션 ID
     * @param pageable  페이지 요청 정보
     * @return 참여 요청 페이지
     */
    Page<JoinRequest> findBySessionId(Long sessionId, Pageable pageable);

    /**
     * 세션 ID와 상태로 참여 요청 목록을 페이지 단위로 조회한다.
     *
     * @param sessionId 세션 ID
     * @param status    요청 상태
     * @param pageable  페이지 요청 정보
     * @return 참여 요청 페이지
     */
    Page<JoinRequest> findBySessionIdAndStatus(Long sessionId, JoinRequestStatus status, Pageable pageable);
}
