package com.dirtypay.domain.session.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 정산 세션 엔티티.
 *
 * <p>정산 그룹을 나타내며, 여러 참여자가 함께 비용을 정산하는 단위이다.
 * Soft Delete를 지원하여 deletedDate가 null이 아닌 경우 삭제된 것으로 간주한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "sessions")
@SQLRestriction("deleted_date IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, unique = true, length = 8)
    private String inviteCode;

    @Builder
    public Session(String title, String description, LocalDate startDate, LocalDate endDate,
                   SessionStatus status, Long ownerId) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status != null ? status : SessionStatus.ACTIVE;
        this.ownerId = ownerId;
        this.inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void update(String title, String description, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void archive() {
        this.status = SessionStatus.ARCHIVED;
    }

    public boolean isActive() {
        return this.status == SessionStatus.ACTIVE;
    }
}
