package com.dirtypay.domain.session.repository;

import com.dirtypay.domain.session.entity.Session;
import com.dirtypay.domain.session.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 세션 리포지토리.
 *
 * <p>세션 엔티티에 대한 데이터 접근을 담당한다.
 * {@code @SQLRestriction}에 의해 삭제된 엔티티가 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * 사용자가 참여 중인(OrgMember로 등록된) 세션을 상태별로 조회한다.
     *
     * <p>세션 생성 시 소유자도 OrgMember로 등록되므로,
     * 소유 세션과 참여 세션을 모두 포함한다.
     * OrgMember.sessionId 직접 필드를 사용하여 Node 경유 없이 조회한다.</p>
     *
     * @param userId 사용자 ID
     * @param status 세션 상태
     * @return 세션 목록
     */
    @Query("SELECT DISTINCT s FROM Session s " +
           "JOIN OrgMember om ON om.sessionId = s.id " +
           "WHERE om.userId = :userId AND s.status = :status")
    List<Session> findByMemberUserIdAndStatus(@Param("userId") Long userId, @Param("status") SessionStatus status);

    /**
     * 초대 코드로 세션을 조회한다.
     *
     * @param inviteCode 초대 코드
     * @return 세션 Optional
     */
    Optional<Session> findByInviteCode(String inviteCode);
}
