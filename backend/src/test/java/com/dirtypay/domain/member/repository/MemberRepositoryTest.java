package com.dirtypay.domain.member.repository;

import com.dirtypay.domain.member.entity.Member;
import com.dirtypay.domain.member.entity.MemberRole;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MemberRepository} 통합 테스트.
 *
 * <p>이메일 기반 조회, 이메일 존재 여부 확인,
 * {@code @SQLRestriction}에 의한 소프트 삭제 필터링을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트 픽스처: 활성 회원
    private Member activeMember;
    // 테스트 픽스처: 비활성 역할 회원 (ADMIN)
    private Member adminMember;
    // 테스트 픽스처: 소프트 삭제된 회원
    private Member deletedMember;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        // 활성 일반 회원
        activeMember = memberRepository.save(Member.builder()
                .email("active@dirtypay.com")
                .password("encoded-password-1")
                .name("활성회원")
                .role(MemberRole.USER)
                .build());

        // 활성 관리자 회원
        adminMember = memberRepository.save(Member.builder()
                .email("admin@dirtypay.com")
                .password("encoded-password-2")
                .name("관리자")
                .role(MemberRole.ADMIN)
                .build());

        // 소프트 삭제된 회원
        deletedMember = memberRepository.save(Member.builder()
                .email("deleted@dirtypay.com")
                .password("encoded-password-3")
                .name("탈퇴회원")
                .role(MemberRole.USER)
                .build());
        deletedMember.delete();
        memberRepository.save(deletedMember);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByEmail 테스트")
    class FindByEmailTest {

        @Test
        @DisplayName("존재하는 이메일로 조회하면 회원이 반환된다")
        void findByEmail_returnsPresent_whenEmailExists() {
            // when
            Optional<Member> result = memberRepository.findByEmail("active@dirtypay.com");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("active@dirtypay.com");
            assertThat(result.get().getName()).isEqualTo("활성회원");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회하면 Optional.empty()가 반환된다")
        void findByEmail_returnsEmpty_whenEmailNotExists() {
            // when
            Optional<Member> result = memberRepository.findByEmail("nonexistent@dirtypay.com");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 회원은 이메일로 조회되지 않는다")
        void findByEmail_excludesDeletedMember() {
            // when
            Optional<Member> result = memberRepository.findByEmail("deleted@dirtypay.com");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail 테스트")
    class ExistsByEmailTest {

        @Test
        @DisplayName("존재하는 이메일이면 true를 반환한다")
        void existsByEmail_returnsTrue_whenEmailExists() {
            // when
            boolean result = memberRepository.existsByEmail("active@dirtypay.com");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 false를 반환한다")
        void existsByEmail_returnsFalse_whenEmailNotExists() {
            // when
            boolean result = memberRepository.existsByEmail("ghost@dirtypay.com");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("@SQLRestriction: 소프트 삭제된 회원의 이메일은 false를 반환한다")
        void existsByEmail_returnsFalse_forDeletedMember() {
            // when
            boolean result = memberRepository.existsByEmail("deleted@dirtypay.com");

            // then
            // 소프트 삭제된 회원의 이메일은 @SQLRestriction에 의해 존재하지 않는 것으로 처리된다
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("@SQLRestriction Soft Delete 필터링 테스트")
    class SoftDeleteFilteringTest {

        @Test
        @DisplayName("findAll: 소프트 삭제된 회원은 전체 목록에서 제외된다")
        void findAll_excludesDeletedMembers() {
            // when
            var allMembers = memberRepository.findAll();

            // then
            // activeMember, adminMember 2명만 조회되어야 한다
            assertThat(allMembers).hasSize(2);
            assertThat(allMembers).extracting(Member::getEmail)
                    .containsExactlyInAnyOrder("active@dirtypay.com", "admin@dirtypay.com")
                    .doesNotContain("deleted@dirtypay.com");
        }

        @Test
        @DisplayName("findById: 소프트 삭제된 회원은 ID로 조회되지 않는다")
        void findById_excludesDeletedMember() {
            // when
            Optional<Member> result = memberRepository.findById(deletedMember.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchByKeyword 접두어 매칭 테스트")
    class SearchByKeywordTest {

        @Test
        @DisplayName("이메일 접두어로 검색하면 해당 회원이 반환된다")
        void searchByKeyword_returnsMembers_whenEmailPrefixMatches() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Member> result = memberRepository.searchByKeyword("active", pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("active@dirtypay.com");
        }

        @Test
        @DisplayName("이름 접두어로 검색하면 해당 회원이 반환된다")
        void searchByKeyword_returnsMembers_whenNamePrefixMatches() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Member> result = memberRepository.searchByKeyword("활성", pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("활성회원");
        }

        @Test
        @DisplayName("접두어가 아닌 중간 문자열로는 검색되지 않는다 (Leading Wildcard 제거 검증)")
        void searchByKeyword_returnsEmpty_whenMidStringDoesNotMatchPrefix() {
            // given - "dirtypay"는 email 중간에 있으나 접두어가 아니다
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Member> result = memberRepository.searchByKeyword("dirtypay", pageable);

            // then
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("일치하는 회원이 없으면 빈 페이지가 반환된다")
        void searchByKeyword_returnsEmptyPage_whenNoMatch() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Member> result = memberRepository.searchByKeyword("nonexistent", pageable);

            // then
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("소프트 삭제된 회원은 검색 결과에서 제외된다")
        void searchByKeyword_excludesDeletedMembers() {
            // given - "탈퇴" 는 소프트 삭제된 회원의 이름 접두어
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<Member> result = memberRepository.searchByKeyword("탈퇴", pageable);

            // then
            assertThat(result.getTotalElements()).isZero();
        }
    }
}
