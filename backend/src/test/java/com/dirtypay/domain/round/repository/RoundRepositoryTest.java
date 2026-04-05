package com.dirtypay.domain.round.repository;

import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundStatus;
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
 * {@link RoundRepository} 통합 테스트.
 *
 * <p>세션별 라운드 조회, sortOrder 기반 정렬, 라운드 수 집계,
 * {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("RoundRepository 통합 테스트")
class RoundRepositoryTest {

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EntityManager entityManager;

    private Session session;
    private Round round1;
    private Round round2;
    private Round round3;
    private Round deletedRound;

    @BeforeEach
    void setUp() {
        // 테스트 격리: 기존 데이터 모두 제거
        roundRepository.deleteAll();
        sessionRepository.deleteAll();

        // 세션 생성
        session = sessionRepository.save(Session.builder()
                .title("테스트 세션")
                .description("라운드 테스트용 세션")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 28))
                .ownerId(1L)
                .build());

        // sortOrder 값을 역순으로 저장하여 정렬 검증을 명확히 함
        round3 = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("3차 디저트")
                .place("디저트숍C")
                .roundDate(LocalDate.of(2026, 2, 10))
                .status(RoundStatus.OPEN)
                .sortOrder(3)
                .build());

        round1 = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("1차 식사")
                .place("식당A")
                .roundDate(LocalDate.of(2026, 2, 10))
                .status(RoundStatus.OPEN)
                .sortOrder(1)
                .build());

        round2 = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("2차 카페")
                .place("카페B")
                .roundDate(LocalDate.of(2026, 2, 10))
                .status(RoundStatus.CLOSED)
                .sortOrder(2)
                .build());

        // 소프트 삭제된 라운드
        deletedRound = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("삭제된 라운드")
                .status(RoundStatus.OPEN)
                .sortOrder(4)
                .build());
        deletedRound.delete();
        roundRepository.save(deletedRound);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findBySessionIdOrderBySortOrderAsc: 세션별 정렬된 라운드 조회")
    class FindBySessionIdOrderBySortOrderAsc {

        @Test
        @DisplayName("sortOrder 오름차순으로 라운드 목록을 반환한다")
        void returnsRoundsInAscendingSortOrder() {
            // when
            List<Round> rounds = roundRepository.findBySessionIdOrderBySortOrderAsc(session.getId());

            // then
            assertThat(rounds).hasSize(3);
            assertThat(rounds).extracting(Round::getSortOrder)
                    .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("첫 번째 라운드는 sortOrder가 가장 작은 라운드이다")
        void firstElementHasLowestSortOrder() {
            // when
            List<Round> rounds = roundRepository.findBySessionIdOrderBySortOrderAsc(session.getId());

            // then
            assertThat(rounds.get(0).getSortOrder()).isEqualTo(1);
            assertThat(rounds.get(0).getTitle()).isEqualTo("1차 식사");
        }

        @Test
        @DisplayName("마지막 라운드는 sortOrder가 가장 큰 라운드이다")
        void lastElementHasHighestSortOrder() {
            // when
            List<Round> rounds = roundRepository.findBySessionIdOrderBySortOrderAsc(session.getId());

            // then
            assertThat(rounds.get(rounds.size() - 1).getSortOrder()).isEqualTo(3);
            assertThat(rounds.get(rounds.size() - 1).getTitle()).isEqualTo("3차 디저트");
        }

        @Test
        @DisplayName("@SQLRestriction: 삭제된 라운드는 정렬 조회에서 제외된다")
        void excludesDeletedRoundsFromSortedQuery() {
            // when
            List<Round> rounds = roundRepository.findBySessionIdOrderBySortOrderAsc(session.getId());

            // then - deletedRound(sortOrder=4)가 포함되지 않아야 함
            assertThat(rounds).hasSize(3);
            assertThat(rounds).extracting(Round::getTitle)
                    .doesNotContain("삭제된 라운드");
        }

        @Test
        @DisplayName("존재하지 않는 세션 ID 조회 시 빈 목록을 반환한다")
        void returnsEmptyListForNonExistentSessionId() {
            // given
            Long nonExistentSessionId = 999L;

            // when
            List<Round> rounds = roundRepository.findBySessionIdOrderBySortOrderAsc(nonExistentSessionId);

            // then
            assertThat(rounds).isEmpty();
        }
    }

    @Nested
    @DisplayName("countBySessionId: 세션별 라운드 수 집계")
    class CountBySessionId {

        @Test
        @DisplayName("세션에 속한 삭제되지 않은 라운드 수를 반환한다")
        void returnsCountOfActiveRoundsInSession() {
            // when
            long count = roundRepository.countBySessionId(session.getId());

            // then - round1, round2, round3 = 3개 (deletedRound 제외)
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("@SQLRestriction: 삭제된 라운드는 카운트에서 제외된다")
        void excludesDeletedRoundsFromCount() {
            // given: deletedRound가 setUp에서 소프트 삭제됨

            // when
            long count = roundRepository.countBySessionId(session.getId());

            // then - 소프트 삭제된 라운드 1개가 제외되어 3이 반환됨
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("존재하지 않는 세션 ID 집계 시 0을 반환한다")
        void returnsZeroForNonExistentSessionId() {
            // given
            Long nonExistentSessionId = 999L;

            // when
            long count = roundRepository.countBySessionId(nonExistentSessionId);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("findBySessionId: 세션별 라운드 목록 조회")
    class FindBySessionId {

        @Test
        @DisplayName("세션에 속한 삭제되지 않은 모든 라운드를 반환한다")
        void returnsAllActiveRoundsForSession() {
            // when
            List<Round> rounds = roundRepository.findBySessionId(session.getId());

            // then
            assertThat(rounds).hasSize(3);
            assertThat(rounds).extracting(Round::getTitle)
                    .containsExactlyInAnyOrder("1차 식사", "2차 카페", "3차 디저트");
        }

        @Test
        @DisplayName("@SQLRestriction: 삭제된 라운드는 조회에서 제외된다")
        void excludesDeletedRoundsFromQuery() {
            // when
            List<Round> rounds = roundRepository.findBySessionId(session.getId());

            // then
            assertThat(rounds).extracting(Round::getTitle)
                    .doesNotContain("삭제된 라운드");
        }

        @Test
        @DisplayName("다른 세션의 라운드는 조회되지 않는다")
        void doesNotReturnRoundsFromOtherSession() {
            // given: 다른 세션에 라운드 추가
            Session otherSession = sessionRepository.save(Session.builder()
                    .title("다른 세션")
                    .ownerId(2L)
                    .build());
            roundRepository.save(Round.builder()
                    .sessionId(otherSession.getId())
                    .title("다른 세션의 라운드")
                    .status(RoundStatus.OPEN)
                    .sortOrder(1)
                    .build());

            entityManager.flush();
            entityManager.clear();

            // when
            List<Round> rounds = roundRepository.findBySessionId(session.getId());

            // then
            assertThat(rounds).hasSize(3);
            assertThat(rounds).extracting(Round::getTitle)
                    .doesNotContain("다른 세션의 라운드");
        }
    }
}
