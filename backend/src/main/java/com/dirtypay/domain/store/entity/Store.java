package com.dirtypay.domain.store.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
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

/**
 * 매장 엔티티.
 *
 * <p>DirtyPay에 등록된 매장 정보를 나타낸다. {@code ownerId}는 매장 소유자인
 * 회원의 ID이며, FK 무결성은 Service 계층에서 검증한다.</p>
 *
 * <p>Soft Delete: {@code deleted_date IS NULL} 조건으로 삭제된 레코드를
 * 자동 제외한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class Store extends BaseEntity {

    /**
     * 매장 소유자 회원 ID. FK → members (검증은 Service에서 수행).
     */
    @Column(nullable = false)
    private Long ownerId;

    /**
     * 매장명. 최대 100자.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 사업자 등록번호. 전체 매장에서 고유해야 한다.
     */
    @Column(unique = true, length = 20)
    private String businessNumber;

    /**
     * 매장 주소. 최대 255자.
     */
    @Column(nullable = false, length = 255)
    private String address;

    /**
     * 매장 전화번호. 최대 20자. 선택 항목.
     */
    @Column(length = 20)
    private String phone;

    /**
     * 매장 소개. 최대 1000자. 선택 항목.
     */
    @Column(length = 1000)
    private String description;

    /**
     * 매장 유형 (POS 연동 여부).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreType storeType;

    /**
     * 매장 운영 상태. 기본값은 {@link StoreStatus#ACTIVE}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status;

    /**
     * POS 연동 키. {@link StoreType#POS_INTEGRATED}인 경우 필수. 최대 100자.
     */
    @Column(length = 100)
    private String posIntegrationKey;

    /**
     * Store 엔티티 생성자.
     *
     * @param ownerId           소유자 회원 ID
     * @param name              매장명
     * @param businessNumber    사업자 등록번호
     * @param address           매장 주소
     * @param phone             매장 전화번호
     * @param description       매장 소개
     * @param storeType         매장 유형
     * @param status            매장 운영 상태
     * @param posIntegrationKey POS 연동 키
     */
    @Builder
    public Store(Long ownerId, String name, String businessNumber, String address,
                 String phone, String description, StoreType storeType,
                 StoreStatus status, String posIntegrationKey) {
        this.ownerId = ownerId;
        this.name = name;
        this.businessNumber = businessNumber;
        this.address = address;
        this.phone = phone;
        this.description = description;
        this.storeType = storeType;
        this.status = status;
        this.posIntegrationKey = posIntegrationKey;
    }

    /**
     * 매장 기본 정보를 수정한다.
     *
     * @param name        변경할 매장명
     * @param address     변경할 주소
     * @param phone       변경할 전화번호
     * @param description 변경할 소개
     */
    public void update(String name, String address, String phone, String description) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.description = description;
    }

    /**
     * 매장 운영 상태를 변경한다.
     *
     * <p>폐업({@link StoreStatus#CLOSED}) 상태에서는 상태 변경이 불가능하다.</p>
     *
     * @param status 변경할 상태
     * @throws BusinessException 이미 폐업한 매장인 경우
     */
    public void changeStatus(StoreStatus status) {
        if (this.status == StoreStatus.CLOSED) {
            throw new BusinessException(ErrorCode.STORE_ALREADY_CLOSED);
        }
        if (this.status == status) {
            return;
        }
        this.status = status;
    }

    /**
     * 매장을 폐업 처리한다.
     *
     * <p>이미 폐업한 매장에 재호출해도 예외가 발생하지 않는다.</p>
     */
    public void close() {
        this.status = StoreStatus.CLOSED;
    }

    /**
     * 매장이 활성 상태인지 검증한다.
     *
     * @throws BusinessException 매장이 {@link StoreStatus#ACTIVE}가 아닌 경우
     */
    public void verifyActive() {
        if (this.status != StoreStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.STORE_NOT_ACTIVE);
        }
    }

}
