package com.dirtypay.domain.group.repository;

import com.dirtypay.domain.group.entity.RoundGroupSharedMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 라운드 그룹 공유 메뉴 리포지토리.
 *
 * <p>{@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface RoundGroupSharedMenuRepository extends JpaRepository<RoundGroupSharedMenu, Long> {

    /**
     * 그룹 ID로 공유 메뉴 목록을 조회한다.
     *
     * @param groupId 그룹 ID
     * @return 공유 메뉴 목록
     */
    List<RoundGroupSharedMenu> findByGroupId(Long groupId);

    /**
     * 그룹 ID 목록으로 공유 메뉴 목록을 일괄 조회한다.
     *
     * @param groupIds 그룹 ID 목록
     * @return 공유 메뉴 목록
     */
    List<RoundGroupSharedMenu> findByGroupIdIn(List<Long> groupIds);
}
