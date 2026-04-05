package com.dirtypay.domain.session.repository;

import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.entity.SessionStatus;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private EntityManager entityManager;

    private Session activeSession;
    private Session archivedSession;
    private Session deletedSession;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();

        activeSession = sessionRepository.save(Session.builder()
                .title("활성 세션")
                .description("활성 상태 세션")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 2, 28))
                .ownerId(1L)
                .build());

        archivedSession = sessionRepository.save(Session.builder()
                .title("보관 세션")
                .description("보관 상태 세션")
                .status(SessionStatus.ARCHIVED)
                .ownerId(1L)
                .build());

        deletedSession = sessionRepository.save(Session.builder()
                .title("삭제된 세션")
                .ownerId(2L)
                .build());
        deletedSession.delete();
        sessionRepository.save(deletedSession);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findById: @SQLRestriction에 의해 삭제되지 않은 세션을 ID로 조회한다")
    void findById_returnsActiveSession() {
        // when
        Optional<Session> found = sessionRepository.findById(activeSession.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("활성 세션");
    }

    @Test
    @DisplayName("findById: @SQLRestriction에 의해 삭제된 세션은 조회되지 않는다")
    void findById_excludesDeletedSession() {
        // when
        Optional<Session> found = sessionRepository.findById(deletedSession.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll: @SQLRestriction에 의해 삭제되지 않은 모든 세션을 조회한다")
    void findAll_returnsAllActive() {
        // when
        List<Session> sessions = sessionRepository.findAll();

        // then
        assertThat(sessions).hasSize(2);
    }

    @Test
    @DisplayName("save: 세션을 저장하고 ID가 자동 생성된다")
    void save_generatesId() {
        // given
        Session session = Session.builder()
                .title("새 세션")
                .ownerId(3L)
                .build();

        // when
        Session saved = sessionRepository.save(session);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getUpdatedDate()).isNotNull();
    }

    @Test
    @DisplayName("startDate, endDate가 정상적으로 저장/조회된다")
    void save_withDateFields() {
        // given
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        Session session = sessionRepository.save(Session.builder()
                .title("날짜 테스트")
                .startDate(start)
                .endDate(end)
                .ownerId(3L)
                .build());

        // when
        Optional<Session> found = sessionRepository.findById(session.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getStartDate()).isEqualTo(start);
        assertThat(found.get().getEndDate()).isEqualTo(end);
    }
}
