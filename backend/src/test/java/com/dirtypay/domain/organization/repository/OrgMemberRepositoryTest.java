package com.dirtypay.domain.organization.repository;

import com.dirtypay.domain.organization.entity.OrgMember;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class OrgMemberRepositoryTest {

    @Autowired
    private OrgMemberRepository orgMemberRepository;

    @Autowired
    private EntityManager entityManager;

    private OrgMember activeMember;
    private OrgMember inactiveMember;
    private OrgMember deletedMember;

    @BeforeEach
    void setUp() {
        orgMemberRepository.deleteAll();

        activeMember = orgMemberRepository.save(OrgMember.builder()
                .sessionId(1L)
                .userId(10L)
                .nickname("홍길동")
                .build());

        inactiveMember = orgMemberRepository.save(OrgMember.builder()
                .sessionId(1L)
                .nickname("비활성멤버")
                .isActive(false)
                .build());

        deletedMember = orgMemberRepository.save(OrgMember.builder()
                .sessionId(1L)
                .nickname("삭제멤버")
                .build());
        deletedMember.delete();
        orgMemberRepository.save(deletedMember);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findById: @SQLRestriction에 의해 삭제되지 않은 멤버를 ID로 조회한다")
    void findById_success() {
        // when
        Optional<OrgMember> found = orgMemberRepository.findById(activeMember.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getNickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("findById: @SQLRestriction에 의해 삭제된 멤버는 조회되지 않는다")
    void findById_excludesDeleted() {
        // when
        Optional<OrgMember> found = orgMemberRepository.findById(deletedMember.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findBySessionId: 세션의 삭제되지 않은 멤버를 직접 sessionId로 조회한다")
    void findBySessionId_success() {
        // given - 다른 세션 멤버 추가
        orgMemberRepository.save(OrgMember.builder()
                .sessionId(2L)
                .nickname("다른세션멤버")
                .build());

        // when
        List<OrgMember> members = orgMemberRepository.findBySessionId(1L);

        // then
        assertThat(members).hasSize(2); // 삭제된 멤버 제외
        assertThat(members).extracting(OrgMember::getNickname)
                .containsExactlyInAnyOrder("홍길동", "비활성멤버");
    }

    @Test
    @DisplayName("비회원(userId null) 멤버가 정상 저장/조회된다")
    void save_guestMember() {
        // given
        OrgMember guest = orgMemberRepository.save(OrgMember.builder()
                .sessionId(1L)
                .nickname("비회원참여자")
                .build());

        // when
        Optional<OrgMember> found = orgMemberRepository.findById(guest.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isNull();
        assertThat(found.get().isLinkedUser()).isFalse();
    }

    @Test
    @DisplayName("save: 멤버 저장 시 ID와 Auditing 필드가 자동 생성된다")
    void save_generatesId() {
        // given
        OrgMember member = OrgMember.builder()
                .sessionId(1L)
                .nickname("새멤버")
                .build();

        // when
        OrgMember saved = orgMemberRepository.save(member);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.isActive()).isTrue();
    }
}
