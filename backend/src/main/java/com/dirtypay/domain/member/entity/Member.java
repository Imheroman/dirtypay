package com.dirtypay.domain.member.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티.
 *
 * <p>시스템의 사용자 정보를 관리하며, 인증 및 정산 참여자 역할을 담당한다.
 * Soft Delete를 지원하여 deletedDate가 null이 아닌 경우 삭제된 것으로 간주한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class Member extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    /**
     * Member 엔티티를 생성한다.
     *
     * @param email        이메일 (고유값)
     * @param password     암호화된 비밀번호
     * @param name         회원 이름
     * @param profileImage 프로필 이미지 URL (nullable)
     * @param role         회원 권한 (null인 경우 USER로 설정)
     */
    @Builder
    public Member(String email, String password, String name, String profileImage, MemberRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileImage = profileImage;
        this.role = role != null ? role : MemberRole.USER;
    }

    /**
     * 회원 프로필 정보를 수정한다.
     *
     * @param name         새로운 이름
     * @param profileImage 새로운 프로필 이미지 URL
     */
    public void updateProfile(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }

    /**
     * 비밀번호를 변경한다.
     *
     * @param newPassword 새로운 암호화된 비밀번호
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
}
