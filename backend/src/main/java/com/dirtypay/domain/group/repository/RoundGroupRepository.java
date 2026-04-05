package com.dirtypay.domain.group.repository;

import com.dirtypay.domain.group.entity.RoundGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 라운드 그룹 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface RoundGroupRepository extends JpaRepository<RoundGroup, Long> {

    /**
     * 라운드 ID로 그룹 목록을 조회한다.
     *
     * @param roundId 라운드 ID
     * @return 그룹 목록
     */
    List<RoundGroup> findByRoundId(Long roundId);

    /**
     * 상위 그룹 ID로 하위 그룹 목록을 조회한다.
     *
     * @param parentGroupId 상위 그룹 ID
     * @return 하위 그룹 목록
     */
    List<RoundGroup> findByParentGroupId(Long parentGroupId);

    /**
     * 여러 라운드 ID에 속한 모든 그룹을 한 번에 조회한다.
     *
     * <p>세션 정산 시 라운드별 반복 조회(N+1)를 단일 IN 쿼리로 대체한다.</p>
     *
     * @param roundIds 라운드 ID 목록
     * @return 그룹 목록
     */
    List<RoundGroup> findByRoundIdIn(List<Long> roundIds);
}
