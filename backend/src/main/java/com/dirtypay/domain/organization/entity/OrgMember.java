package com.dirtypay.domain.organization.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 조직도 참여자 엔티티.
 *
 * <p>세션에 소속되는 참여자를 나타낸다.
 * Auth 도메인의 Member(User)와 구분되는 조직도 전용 참여자이다.</p>
 *
 * <p>userId가 null이면 비회원(미가입) 참여자이며,
 * nickname으로 식별한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "org_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_session_user", columnNames = {"session_id", "user_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class OrgMember extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private boolean isActive;

    @Builder
    public OrgMember(Long sessionId, Long userId, String nickname, Boolean isActive) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.nickname = nickname;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 참여자 정보를 수정한다.
     *
     * @param nickname 닉네임
     * @param isActive 활성 상태 (null이면 변경하지 않음)
     */
    public void update(String nickname, Boolean isActive) {
        this.nickname = nickname;
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    /**
     * 참여자를 비활성화한다.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 참여자를 활성화한다.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 시스템 회원을 연결한다.
     *
     * <p>userId가 null인 OrgMember에 회원 ID를 사후 연결한다.
     * 이미 회원이 연결된 경우에는 재연결할 수 없다.</p>
     *
     * @param userId 연결할 회원 ID (null 불가)
     * @throws BusinessException 이미 회원이 연결된 경우 (MEMBER_ALREADY_LINKED)
     */
    public void linkUser(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (this.isLinkedUser()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_LINKED);
        }
        this.userId = userId;
    }

    /**
     * 회원 연결 여부를 확인한다.
     *
     * @return 시스템 회원과 연결되어 있으면 true
     */
    public boolean isLinkedUser() {
        return this.userId != null;
    }
}
