package com.dirtypay.domain.group.repository;

import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupSharedMenu;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RoundGroupSharedMenuRepository} 단위 테스트.
 *
 * <p>그룹 공유 메뉴 조회, 다중 그룹 일괄 조회, quantity 값 검증,
 * {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class RoundGroupSharedMenuRepositoryTest {

    @Autowired
    private RoundGroupSharedMenuRepository roundGroupSharedMenuRepository;

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private EntityManager entityManager;

    private RoundGroup group1;
    private RoundGroup group2;
    private RoundGroupSharedMenu menu1InGroup1;
    private RoundGroupSharedMenu menu2InGroup1;
    private RoundGroupSharedMenu menu1InGroup2;
    private RoundGroupSharedMenu deletedMenu;

    @BeforeEach
    void setUp() {
        roundGroupSharedMenuRepository.deleteAll();
        roundGroupRepository.deleteAll();

        // 그룹 2개 생성
        group1 = roundGroupRepository.save(RoundGroup.builder()
                .roundId(1L)
                .name("1팀")
                .depth(0)
                .build());

        group2 = roundGroupRepository.save(RoundGroup.builder()
                .roundId(1L)
                .name("2팀")
                .depth(0)
                .build());

        // group1에 공유 메뉴 2개 (다른 수량)
        menu1InGroup1 = roundGroupSharedMenuRepository.save(RoundGroupSharedMenu.builder()
                .groupId(group1.getId())
                .menuId(10L)
                .quantity(2)
                .build());

        menu2InGroup1 = roundGroupSharedMenuRepository.save(RoundGroupSharedMenu.builder()
                .groupId(group1.getId())
                .menuId(20L)
                .quantity(3)
                .build());

        // group2에 공유 메뉴 1개
        menu1InGroup2 = roundGroupSharedMenuRepository.save(RoundGroupSharedMenu.builder()
                .groupId(group2.getId())
                .menuId(10L)
                .quantity(1)
                .build());

        // 소프트 삭제된 공유 메뉴
        deletedMenu = roundGroupSharedMenuRepository.save(RoundGroupSharedMenu.builder()
                .groupId(group1.getId())
                .menuId(30L)
                .quantity(5)
                .build());
        deletedMenu.delete();
        roundGroupSharedMenuRepository.save(deletedMenu);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByGroupId: 그룹 ID로 삭제되지 않은 공유 메뉴 목록을 조회한다")
    void findByGroupId_returnsActiveSharedMenus() {
        // when
        List<RoundGroupSharedMenu> menus = roundGroupSharedMenuRepository.findByGroupId(group1.getId());

        // then
        assertThat(menus).hasSize(2);
        assertThat(menus).extracting(RoundGroupSharedMenu::getMenuId)
                .containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    @DisplayName("findByGroupId: 존재하지 않는 그룹 ID이면 빈 목록을 반환한다")
    void findByGroupId_returnsEmptyWhenGroupNotFound() {
        // when
        List<RoundGroupSharedMenu> menus = roundGroupSharedMenuRepository.findByGroupId(9999L);

        // then
        assertThat(menus).isEmpty();
    }

    @Test
    @DisplayName("findByGroupIdIn: 여러 그룹 ID로 공유 메뉴 목록을 일괄 조회한다")
    void findByGroupIdIn_returnsAllSharedMenusInGroups() {
        // given
        List<Long> groupIds = List.of(group1.getId(), group2.getId());

        // when
        List<RoundGroupSharedMenu> menus = roundGroupSharedMenuRepository.findByGroupIdIn(groupIds);

        // then
        // group1 활성 메뉴 2개 + group2 활성 메뉴 1개 = 총 3개
        assertThat(menus).hasSize(3);
        assertThat(menus).extracting(RoundGroupSharedMenu::getGroupId)
                .containsExactlyInAnyOrder(group1.getId(), group1.getId(), group2.getId());
    }

    @Test
    @DisplayName("findByGroupIdIn: 빈 그룹 ID 목록이면 빈 결과를 반환한다")
    void findByGroupIdIn_returnsEmptyForEmptyGroupIds() {
        // when
        List<RoundGroupSharedMenu> menus = roundGroupSharedMenuRepository.findByGroupIdIn(List.of());

        // then
        assertThat(menus).isEmpty();
    }

    @Test
    @DisplayName("quantity 값이 정확하게 저장·조회된다")
    void findByGroupId_quantityValuesAreCorrect() {
        // when
        List<RoundGroupSharedMenu> menus = roundGroupSharedMenuRepository.findByGroupId(group1.getId());

        // then
        assertThat(menus).extracting(RoundGroupSharedMenu::getQuantity)
                .containsExactlyInAnyOrder(2, 3);
    }

    @Test
    @DisplayName("@SQLRestriction: 소프트 삭제된 공유 메뉴는 모든 조회에서 제외된다")
    void sqlRestriction_excludesDeletedSharedMenus() {
        // when
        List<RoundGroupSharedMenu> group1Menus = roundGroupSharedMenuRepository.findByGroupId(group1.getId());
        List<RoundGroupSharedMenu> allMenus = roundGroupSharedMenuRepository
                .findByGroupIdIn(List.of(group1.getId(), group2.getId()));

        // then - 삭제된 menuId=30 는 결과에 포함되지 않아야 한다
        assertThat(group1Menus).extracting(RoundGroupSharedMenu::getMenuId)
                .doesNotContain(30L);
        assertThat(allMenus).extracting(RoundGroupSharedMenu::getMenuId)
                .doesNotContain(30L);
    }
}
