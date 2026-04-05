package com.dirtypay.domain.group.repository;

import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
import com.dirtypay.domain.round.repository.RoundRepository;
import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.repository.SessionRepository;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RoundGroupRepository} 통합 테스트.
 *
 * <p>라운드별 그룹 조회, 상위 그룹 기반 하위 그룹 조회, 루트 그룹(parentGroupId=null) 조회,
 * self-join 계층 구조, {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("RoundGroupRepository 통합 테스트")
class RoundGroupRepositoryTest {

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EntityManager entityManager;

    private Round round;
    private RoundGroup rootGroup1;
    private RoundGroup rootGroup2;
    private RoundGroup childGroup1;
    private RoundGroup childGroup2;
    private RoundGroup deletedGroup;

    @BeforeEach
    void setUp() {
        // 테스트 격리: 기존 데이터 모두 제거
        roundGroupRepository.deleteAll();
        roundRepository.deleteAll();
        sessionRepository.deleteAll();

        // 세션 및 라운드 생성
        Session session = sessionRepository.save(Session.builder()
                .title("테스트 세션")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 28))
                .ownerId(1L)
                .build());

        round = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("1차 식사")
                .place("식당A")
                .roundDate(LocalDate.of(2026, 2, 10))
                .status(RoundStatus.OPEN)
                .sortOrder(1)
                .build());

        // 계층 구조 구성:
        // rootGroup1 (depth=0, parentGroupId=null)
        //   └─ childGroup1 (depth=1, parentGroupId=rootGroup1.id)
        //   └─ childGroup2 (depth=1, parentGroupId=rootGroup1.id)
        // rootGroup2 (depth=0, parentGroupId=null)
        // deletedGroup (소프트 삭제됨)

        rootGroup1 = roundGroupRepository.save(RoundGroup.builder()
                .roundId(round.getId())
                .parentGroupId(null)
                .name("메인 그룹")
                .depth(0)
                .build());

        rootGroup2 = roundGroupRepository.save(RoundGroup.builder()
                .roundId(round.getId())
                .parentGroupId(null)
                .name("서브 그룹")
                .depth(0)
                .build());

        childGroup1 = roundGroupRepository.save(RoundGroup.builder()
                .roundId(round.getId())
                .parentGroupId(rootGroup1.getId())
                .name("메인 그룹 - 팀A")
                .depth(1)
                .build());

        childGroup2 = roundGroupRepository.save(RoundGroup.builder()
                .roundId(round.getId())
                .parentGroupId(rootGroup1.getId())
                .name("메인 그룹 - 팀B")
                .depth(1)
                .build());

        // 소프트 삭제된 그룹
        deletedGroup = roundGroupRepository.save(RoundGroup.builder()
                .roundId(round.getId())
                .parentGroupId(null)
                .name("삭제된 그룹")
                .depth(0)
                .build());
        deletedGroup.delete();
        roundGroupRepository.save(deletedGroup);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByRoundId: 라운드별 그룹 목록 조회")
    class FindByRoundId {

        @Test
        @DisplayName("라운드에 속한 삭제되지 않은 모든 그룹을 반환한다")
        void returnsAllActiveGroupsForRound() {
            // when
            List<RoundGroup> groups = roundGroupRepository.findByRoundId(round.getId());

            // then - rootGroup1, rootGroup2, childGroup1, childGroup2 = 4개 (deletedGroup 제외)
            assertThat(groups).hasSize(4);
            assertThat(groups).extracting(RoundGroup::getName)
                    .containsExactlyInAnyOrder("메인 그룹", "서브 그룹", "메인 그룹 - 팀A", "메인 그룹 - 팀B");
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 그룹은 조회에서 제외된다")
        void excludesDeletedGroupsFromQuery() {
            // when
            List<RoundGroup> groups = roundGroupRepository.findByRoundId(round.getId());

            // then - deletedGroup이 포함되지 않아야 함
            assertThat(groups).extracting(RoundGroup::getName)
                    .doesNotContain("삭제된 그룹");
        }

        @Test
        @DisplayName("존재하지 않는 라운드 ID 조회 시 빈 목록을 반환한다")
        void returnsEmptyListForNonExistentRoundId() {
            // given
            Long nonExistentRoundId = 999L;

            // when
            List<RoundGroup> groups = roundGroupRepository.findByRoundId(nonExistentRoundId);

            // then
            assertThat(groups).isEmpty();
        }

        @Test
        @DisplayName("다른 라운드의 그룹은 조회되지 않는다")
        void doesNotReturnGroupsFromOtherRound() {
            // given: 다른 라운드에 그룹 추가
            Session otherSession = sessionRepository.save(Session.builder()
                    .title("다른 세션")
                    .ownerId(2L)
                    .build());
            Round otherRound = roundRepository.save(Round.builder()
                    .sessionId(otherSession.getId())
                    .title("다른 라운드")
                    .status(RoundStatus.OPEN)
                    .sortOrder(1)
                    .build());
            roundGroupRepository.save(RoundGroup.builder()
                    .roundId(otherRound.getId())
                    .parentGroupId(null)
                    .name("다른 라운드 그룹")
                    .depth(0)
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            List<RoundGroup> groups = roundGroupRepository.findByRoundId(round.getId());

            // then
            assertThat(groups).hasSize(4);
            assertThat(groups).extracting(RoundGroup::getName)
                    .doesNotContain("다른 라운드 그룹");
        }
    }

    @Nested
    @DisplayName("findByParentGroupId: 상위 그룹별 하위 그룹 조회")
    class FindByParentGroupId {

        @Test
        @DisplayName("상위 그룹 ID로 하위 그룹 목록을 반환한다")
        void returnsChildGroupsForParentGroup() {
            // when
            List<RoundGroup> children = roundGroupRepository.findByParentGroupId(rootGroup1.getId());

            // then
            assertThat(children).hasSize(2);
            assertThat(children).extracting(RoundGroup::getName)
                    .containsExactlyInAnyOrder("메인 그룹 - 팀A", "메인 그룹 - 팀B");
        }

        @Test
        @DisplayName("하위 그룹이 없는 상위 그룹 조회 시 빈 목록을 반환한다")
        void returnsEmptyListForGroupWithNoChildren() {
            // when - rootGroup2는 하위 그룹이 없음
            List<RoundGroup> children = roundGroupRepository.findByParentGroupId(rootGroup2.getId());

            // then
            assertThat(children).isEmpty();
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 하위 그룹은 조회에서 제외된다")
        void excludesDeletedChildGroups() {
            // given: childGroup1을 소프트 삭제
            RoundGroup childToDelete = roundGroupRepository.findById(childGroup1.getId())
                    .orElseThrow();
            childToDelete.delete();
            roundGroupRepository.save(childToDelete);
            entityManager.flush();
            entityManager.clear();

            // when
            List<RoundGroup> children = roundGroupRepository.findByParentGroupId(rootGroup1.getId());

            // then - childGroup2만 남아야 함
            assertThat(children).hasSize(1);
            assertThat(children.get(0).getName()).isEqualTo("메인 그룹 - 팀B");
        }

        @Test
        @DisplayName("존재하지 않는 상위 그룹 ID 조회 시 빈 목록을 반환한다")
        void returnsEmptyListForNonExistentParentGroupId() {
            // given
            Long nonExistentParentId = 999L;

            // when
            List<RoundGroup> children = roundGroupRepository.findByParentGroupId(nonExistentParentId);

            // then
            assertThat(children).isEmpty();
        }
    }

    @Nested
    @DisplayName("루트 그룹(parentGroupId=null) 조회")
    class RootGroupQuery {

        @Test
        @DisplayName("parentGroupId가 null인 루트 그룹만 조회된다")
        void returnsOnlyRootGroups() {
            // when - JPA는 null 파라미터를 IS NULL 조건으로 변환
            List<RoundGroup> rootGroups = roundGroupRepository.findByParentGroupId(null);

            // then - rootGroup1, rootGroup2 = 2개 (childGroup1, childGroup2 제외)
            assertThat(rootGroups).hasSize(2);
            assertThat(rootGroups).extracting(RoundGroup::getName)
                    .containsExactlyInAnyOrder("메인 그룹", "서브 그룹");
        }

        @Test
        @DisplayName("루트 그룹의 depth는 0이다")
        void rootGroupsHaveDepthZero() {
            // when
            List<RoundGroup> rootGroups = roundGroupRepository.findByParentGroupId(null);

            // then
            assertThat(rootGroups).allSatisfy(group ->
                    assertThat(group.getDepth()).isZero()
            );
        }

        @Test
        @DisplayName("하위 그룹의 depth는 0보다 크다")
        void childGroupsHavePositiveDepth() {
            // when
            List<RoundGroup> childGroups = roundGroupRepository.findByParentGroupId(rootGroup1.getId());

            // then
            assertThat(childGroups).allSatisfy(group ->
                    assertThat(group.getDepth()).isGreaterThan(0)
            );
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 루트 그룹은 null 조회에서 제외된다")
        void excludesDeletedRootGroupsFromNullParentQuery() {
            // given: deletedGroup(parentGroupId=null)이 setUp에서 소프트 삭제됨

            // when
            List<RoundGroup> rootGroups = roundGroupRepository.findByParentGroupId(null);

            // then - 삭제된 루트 그룹이 제외되어 2개만 반환됨
            assertThat(rootGroups).hasSize(2);
            assertThat(rootGroups).extracting(RoundGroup::getName)
                    .doesNotContain("삭제된 그룹");
        }
    }
}
