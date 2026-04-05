package com.dirtypay.domain.group.repository;

import com.dirtypay.domain.group.entity.RoundGroup;
import com.dirtypay.domain.group.entity.RoundGroupMember;
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

/**
 * {@link RoundGroupMemberRepository} 단위 테스트.
 *
 * <p>그룹 멤버 목록 조회, 다중 그룹 일괄 조회, 특정 멤버 조회,
 * 존재 여부 확인, {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class RoundGroupMemberRepositoryTest {

    @Autowired
    private RoundGroupMemberRepository roundGroupMemberRepository;

    @Autowired
    private RoundGroupRepository roundGroupRepository;

    @Autowired
    private EntityManager entityManager;

    private RoundGroup group1;
    private RoundGroup group2;
    private RoundGroupMember member1InGroup1;
    private RoundGroupMember member2InGroup1;
    private RoundGroupMember member1InGroup2;
    private RoundGroupMember deletedMember;

    @BeforeEach
    void setUp() {
        roundGroupMemberRepository.deleteAll();
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

        // group1에 멤버 2명
        member1InGroup1 = roundGroupMemberRepository.save(RoundGroupMember.builder()
                .groupId(group1.getId())
                .orgMemberId(100L)
                .build());

        member2InGroup1 = roundGroupMemberRepository.save(RoundGroupMember.builder()
                .groupId(group1.getId())
                .orgMemberId(200L)
                .build());

        // group2에 멤버 1명
        member1InGroup2 = roundGroupMemberRepository.save(RoundGroupMember.builder()
                .groupId(group2.getId())
                .orgMemberId(100L)
                .build());

        // 소프트 삭제된 멤버
        deletedMember = roundGroupMemberRepository.save(RoundGroupMember.builder()
                .groupId(group1.getId())
                .orgMemberId(300L)
                .build());
        deletedMember.delete();
        roundGroupMemberRepository.save(deletedMember);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByGroupId: 그룹 ID로 삭제되지 않은 멤버 목록을 조회한다")
    void findByGroupId_returnsActiveMembers() {
        // when
        List<RoundGroupMember> members = roundGroupMemberRepository.findByGroupId(group1.getId());

        // then
        assertThat(members).hasSize(2);
        assertThat(members).extracting(RoundGroupMember::getOrgMemberId)
                .containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    @DisplayName("findByGroupId: 존재하지 않는 그룹 ID이면 빈 목록을 반환한다")
    void findByGroupId_returnsEmptyWhenGroupNotFound() {
        // when
        List<RoundGroupMember> members = roundGroupMemberRepository.findByGroupId(9999L);

        // then
        assertThat(members).isEmpty();
    }

    @Test
    @DisplayName("findByGroupIdIn: 여러 그룹 ID로 멤버 목록을 일괄 조회한다")
    void findByGroupIdIn_returnsAllMembersInGroups() {
        // given
        List<Long> groupIds = List.of(group1.getId(), group2.getId());

        // when
        List<RoundGroupMember> members = roundGroupMemberRepository.findByGroupIdIn(groupIds);

        // then
        // group1에 활성 멤버 2명, group2에 활성 멤버 1명 = 총 3명
        assertThat(members).hasSize(3);
        assertThat(members).extracting(RoundGroupMember::getGroupId)
                .containsExactlyInAnyOrder(group1.getId(), group1.getId(), group2.getId());
    }

    @Test
    @DisplayName("findByGroupIdIn: 빈 그룹 ID 목록이면 빈 결과를 반환한다")
    void findByGroupIdIn_returnsEmptyForEmptyGroupIds() {
        // when
        List<RoundGroupMember> members = roundGroupMemberRepository.findByGroupIdIn(List.of());

        // then
        assertThat(members).isEmpty();
    }

    @Test
    @DisplayName("findByGroupIdAndOrgMemberId: 그룹+멤버 조합으로 특정 멤버를 조회한다")
    void findByGroupIdAndOrgMemberId_returnsTargetMember() {
        // when
        Optional<RoundGroupMember> found = roundGroupMemberRepository
                .findByGroupIdAndOrgMemberId(group1.getId(), 100L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getGroupId()).isEqualTo(group1.getId());
        assertThat(found.get().getOrgMemberId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("findByGroupIdAndOrgMemberId: 존재하지 않는 멤버이면 Optional.empty()를 반환한다")
    void findByGroupIdAndOrgMemberId_returnsEmptyWhenNotFound() {
        // when
        Optional<RoundGroupMember> found = roundGroupMemberRepository
                .findByGroupIdAndOrgMemberId(group1.getId(), 9999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByGroupIdAndOrgMemberId: 그룹에 멤버가 존재하면 true를 반환한다")
    void existsByGroupIdAndOrgMemberId_returnsTrueWhenExists() {
        // when
        boolean exists = roundGroupMemberRepository
                .existsByGroupIdAndOrgMemberId(group1.getId(), 100L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByGroupIdAndOrgMemberId: 그룹에 멤버가 없으면 false를 반환한다")
    void existsByGroupIdAndOrgMemberId_returnsFalseWhenNotExists() {
        // when
        boolean exists = roundGroupMemberRepository
                .existsByGroupIdAndOrgMemberId(group1.getId(), 9999L);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("@SQLRestriction: 소프트 삭제된 멤버는 모든 조회에서 제외된다")
    void sqlRestriction_excludesDeletedMembers() {
        // when
        List<RoundGroupMember> membersInGroup1 = roundGroupMemberRepository.findByGroupId(group1.getId());
        boolean deletedMemberExists = roundGroupMemberRepository
                .existsByGroupIdAndOrgMemberId(group1.getId(), 300L);
        Optional<RoundGroupMember> deletedFound = roundGroupMemberRepository
                .findByGroupIdAndOrgMemberId(group1.getId(), 300L);

        // then
        assertThat(membersInGroup1).extracting(RoundGroupMember::getOrgMemberId)
                .doesNotContain(300L);
        assertThat(deletedMemberExists).isFalse();
        assertThat(deletedFound).isEmpty();
    }
}
