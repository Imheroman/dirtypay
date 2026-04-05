package com.dirtypay.domain.round.repository;

import com.dirtypay.domain.round.entity.Round;
import com.dirtypay.domain.round.entity.RoundParticipant;
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
 * {@link RoundParticipantRepository} 통합 테스트.
 *
 * <p>라운드별 참여자 조회, 참여자 수 집계,
 * {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
@DisplayName("RoundParticipantRepository 통합 테스트")
class RoundParticipantRepositoryTest {

    @Autowired
    private RoundParticipantRepository roundParticipantRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EntityManager entityManager;

    private Round round;
    private Round emptyRound;
    private RoundParticipant participant1;
    private RoundParticipant participant2;
    private RoundParticipant participant3;
    private RoundParticipant deletedParticipant;

    @BeforeEach
    void setUp() {
        // 테스트 격리: 기존 데이터 모두 제거
        roundParticipantRepository.deleteAll();
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

        // 참여자가 없는 라운드
        emptyRound = roundRepository.save(Round.builder()
                .sessionId(session.getId())
                .title("빈 라운드")
                .status(RoundStatus.OPEN)
                .sortOrder(2)
                .build());

        // round에 참여자 3명 저장
        participant1 = roundParticipantRepository.save(RoundParticipant.builder()
                .roundId(round.getId())
                .orgMemberId(101L)
                .isExcluded(false)
                .build());

        participant2 = roundParticipantRepository.save(RoundParticipant.builder()
                .roundId(round.getId())
                .orgMemberId(102L)
                .isExcluded(false)
                .build());

        participant3 = roundParticipantRepository.save(RoundParticipant.builder()
                .roundId(round.getId())
                .orgMemberId(103L)
                .isExcluded(true)
                .build());

        // 소프트 삭제된 참여자
        deletedParticipant = roundParticipantRepository.save(RoundParticipant.builder()
                .roundId(round.getId())
                .orgMemberId(104L)
                .isExcluded(false)
                .build());
        deletedParticipant.delete();
        roundParticipantRepository.save(deletedParticipant);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByRoundId: 라운드별 참여자 목록 조회")
    class FindByRoundId {

        @Test
        @DisplayName("라운드에 속한 삭제되지 않은 모든 참여자를 반환한다")
        void returnsAllActiveParticipantsForRound() {
            // when
            List<RoundParticipant> participants = roundParticipantRepository.findByRoundId(round.getId());

            // then
            assertThat(participants).hasSize(3);
            assertThat(participants).extracting(RoundParticipant::getOrgMemberId)
                    .containsExactlyInAnyOrder(101L, 102L, 103L);
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 참여자는 조회에서 제외된다")
        void excludesDeletedParticipants() {
            // when
            List<RoundParticipant> participants = roundParticipantRepository.findByRoundId(round.getId());

            // then - deletedParticipant(orgMemberId=104)가 포함되지 않아야 함
            assertThat(participants).extracting(RoundParticipant::getOrgMemberId)
                    .doesNotContain(104L);
        }

        @Test
        @DisplayName("참여자가 없는 라운드 조회 시 빈 목록을 반환한다")
        void returnsEmptyListForRoundWithNoParticipants() {
            // when
            List<RoundParticipant> participants = roundParticipantRepository.findByRoundId(emptyRound.getId());

            // then
            assertThat(participants).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 라운드 ID 조회 시 빈 목록을 반환한다")
        void returnsEmptyListForNonExistentRoundId() {
            // given
            Long nonExistentRoundId = 999L;

            // when
            List<RoundParticipant> participants = roundParticipantRepository.findByRoundId(nonExistentRoundId);

            // then
            assertThat(participants).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByRoundId: 라운드별 참여자 수 집계")
    class CountByRoundId {

        @Test
        @DisplayName("라운드에 속한 삭제되지 않은 참여자 수를 반환한다")
        void returnsCountOfActiveParticipantsInRound() {
            // when
            long count = roundParticipantRepository.countByRoundId(round.getId());

            // then - participant1, participant2, participant3 = 3명 (deletedParticipant 제외)
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 참여자는 카운트에서 제외된다")
        void excludesDeletedParticipantsFromCount() {
            // given: deletedParticipant가 setUp에서 소프트 삭제됨

            // when
            long count = roundParticipantRepository.countByRoundId(round.getId());

            // then - 삭제된 참여자 1명이 제외되어 3이 반환됨
            assertThat(count).isEqualTo(3L);
        }

        @Test
        @DisplayName("참여자가 없는 라운드 집계 시 0을 반환한다")
        void returnsZeroForRoundWithNoParticipants() {
            // when
            long count = roundParticipantRepository.countByRoundId(emptyRound.getId());

            // then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("존재하지 않는 라운드 ID 집계 시 0을 반환한다")
        void returnsZeroForNonExistentRoundId() {
            // given
            Long nonExistentRoundId = 999L;

            // when
            long count = roundParticipantRepository.countByRoundId(nonExistentRoundId);

            // then
            assertThat(count).isZero();
        }
    }
}
