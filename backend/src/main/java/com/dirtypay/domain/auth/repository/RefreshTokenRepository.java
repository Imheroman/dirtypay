package com.dirtypay.domain.auth.repository;

import com.dirtypay.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 저장소 인터페이스.
 *
 * <p>Refresh Token의 CRUD 및 조회 기능을 제공한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 값으로 RefreshToken을 조회한다.
     *
     * @param token Refresh Token 값
     * @return RefreshToken Optional
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 회원 ID로 RefreshToken을 조회한다.
     *
     * @param memberId 회원 ID
     * @return RefreshToken Optional
     */
    Optional<RefreshToken> findByMemberId(Long memberId);

    /**
     * 회원 ID로 모든 RefreshToken을 삭제한다.
     * 로그아웃 시 해당 회원의 모든 토큰을 무효화한다.
     *
     * @param memberId 회원 ID
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    /**
     * 토큰 값으로 RefreshToken을 삭제한다.
     *
     * @param token Refresh Token 값
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
    void deleteByToken(@Param("token") String token);

    /**
     * 만료된 모든 토큰을 삭제한다.
     * 스케줄러에서 주기적으로 호출하여 만료된 토큰을 정리한다.
     *
     * @param now 현재 시간
     * @return 삭제된 토큰 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteAllExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 특정 회원의 RefreshToken 존재 여부를 확인한다.
     *
     * @param memberId 회원 ID
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByMemberId(Long memberId);

    /**
     * 특정 토큰의 존재 여부를 확인한다.
     *
     * @param token Refresh Token 값
     * @return 존재하면 true, 아니면 false
     */
    boolean existsByToken(String token);
}
