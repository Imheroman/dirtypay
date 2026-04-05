package com.dirtypay.domain.round.repository;

import com.dirtypay.domain.round.entity.RoundParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * 라운드 참여자 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface RoundParticipantRepository extends JpaRepository<RoundParticipant, Long> {

    /**
     * 라운드 ID로 참여자 목록을 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 참여자 목록
     */
    List<RoundParticipant> findByRoundId(Long roundId);

    /**
     * 라운드 ID로 참여자 수를 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 참여자 수
     */
    long countByRoundId(Long roundId);

    /**
     * 해당 라운드에 특정 OrgMember의 참여자 레코드가 존재하는지 확인한다.
     *
     * @param roundId     라운드 ID
     * @param orgMemberId 조직 멤버 ID
     * @return 존재하면 true
     */
    boolean existsByRoundIdAndOrgMemberId(Long roundId, Long orgMemberId);

    /**
     * 여러 라운드 ID 중 특정 OrgMember가 이미 참여 중인 라운드 ID 집합을 한 번에 조회한다.
     *
     * <p>IN 쿼리를 사용하여 N번 반복 조회를 단일 쿼리로 대체한다.</p>
     *
     * @param roundIds    조회 대상 라운드 ID 목록
     * @param orgMemberId 조직 멤버 ID
     * @return 이미 참여 중인 라운드 ID 집합
     */
    @Query("SELECT rp.roundId FROM RoundParticipant rp WHERE rp.roundId IN :roundIds AND rp.orgMemberId = :orgMemberId")
    Set<Long> findRoundIdsByRoundIdInAndOrgMemberId(@Param("roundIds") List<Long> roundIds,
                                                    @Param("orgMemberId") Long orgMemberId);

    /**
     * 여러 라운드 ID에 속한 모든 참여자를 한 번에 조회한다.
     *
     * <p>세션 정산 시 라운드별 반복 조회(N+1)를 단일 IN 쿼리로 대체한다.</p>
     *
     * @param roundIds 라운드 ID 목록
     * @return 참여자 목록
     */
    List<RoundParticipant> findByRoundIdIn(List<Long> roundIds);
}
