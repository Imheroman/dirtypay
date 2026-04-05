package com.dirtypay.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 Entity의 공통 필드를 정의하는 추상 클래스.
 *
 * <p>다음 필드를 공통으로 제공한다:</p>
 * <ul>
 *   <li>id: Long 타입 Auto Increment PK</li>
 *   <li>createdDate: 생성 일시</li>
 *   <li>updatedDate: 수정 일시</li>
 *   <li>deletedDate: 삭제 일시 (Soft Delete)</li>
 * </ul>
 *
 * <p>각 엔티티 클래스에 {@code @SQLRestriction("deleted_date IS NULL")}을 적용하여
 * 모든 쿼리에서 삭제된 엔티티를 자동으로 제외한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @Column
    private LocalDateTime deletedDate;

    public void delete() {
        this.deletedDate = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedDate != null;
    }
}
