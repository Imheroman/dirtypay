package com.dirtypay.domain.round.repository;

import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 라운드 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface RoundRepository extends JpaRepository<Round, Long> {

    /**
     * 세션 ID로 라운드 목록을 정렬 순서로 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 라운드 목록
     */
    List<Round> findBySessionIdOrderBySortOrderAsc(Long sessionId);

    /**
     * 세션에 속한 라운드 수를 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 라운드 수
     */
    long countBySessionId(Long sessionId);

    /**
     * 세션 ID로 라운드 목록을 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 라운드 목록
     */
    List<Round> findBySessionId(Long sessionId);

    /**
     * 세션 ID와 상태로 라운드 목록을 조회한다.
     *
     * @param sessionId 세션 ID
     * @param status    라운드 상태
     * @return 해당 상태의 라운드 목록
     */
    List<Round> findBySessionIdAndStatus(Long sessionId, RoundStatus status);
}
