package com.dirtypay.domain.organization.repository;

import com.dirtypay.domain.organization.entity.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 조직도 참여자 리포지토리.
 *
 * <p>OrgMember 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.
 * sessionId 직접 필드를 사용하여 Node 경유 없이 조회한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface OrgMemberRepository extends JpaRepository<OrgMember, Long> {

    /**
     * 세션 ID로 해당 세션의 모든 참여자를 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 참여자 목록
     */
    List<OrgMember> findBySessionId(Long sessionId);

    /**
     * 세션에 속한 참여자 수를 조회한다.
     *
     * @param sessionId 세션 ID
     * @return 참여자 수
     */
    long countBySessionId(Long sessionId);

    /**
     * 세션 ID와 사용자 ID로 조직 멤버를 조회한다.
     *
     * @param sessionId 세션 ID
     * @param userId    사용자 ID
     * @return 조직 멤버 (Optional)
     */
    Optional<OrgMember> findBySessionIdAndUserId(Long sessionId, Long userId);

    /**
     * 세션 ID와 닉네임으로 미연결(userId가 null인) 조직 멤버를 조회한다.
     *
     * <p>회원 가입 전에 조직도에 등록된 참여자를 닉네임 기반으로 매칭할 때 사용한다.</p>
     *
     * @param sessionId 세션 ID
     * @param nickname  닉네임
     * @return 미연결 조직 멤버 (Optional)
     */
    Optional<OrgMember> findBySessionIdAndNicknameAndUserIdIsNull(Long sessionId, String nickname);
}
