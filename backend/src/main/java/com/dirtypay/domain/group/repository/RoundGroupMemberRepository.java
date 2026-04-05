package com.dirtypay.domain.group.repository;

import com.dirtypay.domain.group.entity.RoundGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 라운드 그룹 멤버 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface RoundGroupMemberRepository extends JpaRepository<RoundGroupMember, Long> {

    /**
     * 그룹 ID로 멤버 목록을 조회한다.
     *
     * @param groupId 그룹 ID
     * @return 멤버 목록
     */
    List<RoundGroupMember> findByGroupId(Long groupId);

    /**
     * 그룹 ID 목록으로 멤버 목록을 일괄 조회한다.
     *
     * @param groupIds 그룹 ID 목록
     * @return 멤버 목록
     */
    List<RoundGroupMember> findByGroupIdIn(List<Long> groupIds);

    /**
     * 그룹 ID와 조직 멤버 ID로 멤버를 조회한다.
     *
     * @param groupId     그룹 ID
     * @param orgMemberId 조직 멤버 ID
     * @return 그룹 멤버 (Optional)
     */
    Optional<RoundGroupMember> findByGroupIdAndOrgMemberId(Long groupId, Long orgMemberId);

    /**
     * 그룹에 해당 조직 멤버가 존재하는지 확인한다.
     *
     * @param groupId     그룹 ID
     * @param orgMemberId 조직 멤버 ID
     * @return 존재 여부
     */
    boolean existsByGroupIdAndOrgMemberId(Long groupId, Long orgMemberId);

    /**
     * 라운드 내에서 해당 조직 멤버가 이미 다른 그룹에 참여 중인지 확인한다.
     *
     * <p>{@code @SQLRestriction}은 JPQL theta-join에 자동 적용되지 않으므로
     * {@code deletedDate IS NULL} 조건을 명시한다.</p>
     *
     * @param roundId     라운드 ID
     * @param orgMemberId 조직 멤버 ID
     * @return 참여 여부
     */
    @Query("SELECT CASE WHEN COUNT(rgm) > 0 THEN true ELSE false END " +
            "FROM RoundGroupMember rgm " +
            "JOIN RoundGroup rg ON rgm.groupId = rg.id " +
            "WHERE rg.roundId = :roundId " +
            "AND rgm.orgMemberId = :orgMemberId " +
            "AND rg.deletedDate IS NULL " +
            "AND rgm.deletedDate IS NULL")
    boolean existsByRoundIdAndOrgMemberId(@Param("roundId") Long roundId, @Param("orgMemberId") Long orgMemberId);
}
