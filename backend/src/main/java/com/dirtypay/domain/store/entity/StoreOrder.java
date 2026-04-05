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

import java.math.BigDecimal;

/**
 * 매장 주문 엔티티.
 *
 * <p>매장({@code storeId})에서 특정 메뉴({@code menuId})에 대해 생성된 주문을 나타낸다.
 * {@code orderNumber}는 UUID 형식으로 Service 계층에서 생성하며,
 * 전체 매장 주문에서 고유하다.</p>
 *
 * <p>상태 전이:</p>
 * <pre>
 * PENDING → CONFIRMED → COMPLETED
 *         ↓
 *      CANCELLED
 * </pre>
 *
 * <p>Soft Delete: {@code deleted_date IS NULL} 조건으로 삭제된 레코드를
 * 자동 제외한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "store_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class StoreOrder extends BaseEntity {

    /**
     * 소속 매장 ID. FK → stores.
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     * 주문 대상 메뉴 ID. FK → store_menus.
     */
    @Column(nullable = false)
    private Long menuId;

    /**
     * 주문 수량. 1 이상이어야 한다.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * 주문 총 금액. menu.price * quantity.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    /**
     * 주문 처리 상태.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreOrderStatus status;

    /**
     * 주문 번호. UUID 형식, 전체에서 고유.
     */
    @Column(nullable = false, unique = true, length = 36)
    private String orderNumber;

    /**
     * 주문자 이름. 최대 50자. 선택 항목.
     */
    @Column(length = 50)
    private String customerName;

    /**
     * 주문자 전화번호. 최대 20자. 선택 항목.
     */
    @Column(length = 20)
    private String customerPhone;

    /**
     * 주문자 회원 ID. 비회원 주문 시 null. FK → members (검증은 Service에서 수행).
     */
    @Column
    private Long memberId;

    /**
     * 주문 시점 메뉴 단가 스냅샷. 가격 변동과 무관하게 주문 당시 가격을 보존한다.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 주문 시점 메뉴명 스냅샷. 메뉴명 변경과 무관하게 주문 당시 이름을 보존한다.
     */
    @Column(nullable = false, length = 100)
    private String menuName;

    /**
     * StoreOrder 엔티티 생성자.
     *
     * @param storeId       소속 매장 ID
     * @param menuId        주문 메뉴 ID
     * @param quantity      주문 수량
     * @param totalPrice    주문 총 금액
     * @param orderNumber   주문 번호 (UUID)
     * @param customerName  주문자 이름
     * @param customerPhone 주문자 전화번호
     * @param memberId      주문자 회원 ID (비회원 주문 시 null)
     * @param unitPrice     주문 시점 메뉴 단가 스냅샷
     * @param menuName      주문 시점 메뉴명 스냅샷
     */
    @Builder
    public StoreOrder(Long storeId, Long menuId, int quantity, BigDecimal totalPrice,
                      String orderNumber, String customerName, String customerPhone,
                      Long memberId, BigDecimal unitPrice, String menuName) {
        this.storeId = storeId;
        this.menuId = menuId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = StoreOrderStatus.PENDING;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.memberId = memberId;
        this.unitPrice = unitPrice;
        this.menuName = menuName;
    }

    /**
     * 주문을 확인 처리한다.
     *
     * <p>{@link StoreOrderStatus#PENDING} 상태에서만 호출 가능하다.</p>
     *
     * @throws BusinessException 수정 불가능한 상태인 경우
     */
    public void confirm() {
        if (this.status != StoreOrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.STORE_ORDER_NOT_MODIFIABLE);
        }
        this.status = StoreOrderStatus.CONFIRMED;
    }

    /**
     * 주문을 완료 처리한다.
     *
     * <p>{@link StoreOrderStatus#CONFIRMED} 상태에서만 완료 처리가 가능하다.</p>
     *
     * @throws BusinessException 주문이 CONFIRMED 상태가 아닌 경우
     */
    public void complete() {
        if (this.status != StoreOrderStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.STORE_ORDER_NOT_MODIFIABLE);
        }
        this.status = StoreOrderStatus.COMPLETED;
    }

    /**
     * 주문을 취소 처리한다.
     *
     * <p>{@link StoreOrderStatus#PENDING} 또는 {@link StoreOrderStatus#CONFIRMED}
     * 상태에서만 취소 가능하다.</p>
     *
     * @throws BusinessException 수정 불가능한 상태인 경우
     */
    public void cancel() {
        verifyModifiable();
        this.status = StoreOrderStatus.CANCELLED;
    }

    /**
     * 주문이 변경 가능한 상태인지 검증한다.
     *
     * <p>{@link StoreOrderStatus#COMPLETED} 또는 {@link StoreOrderStatus#CANCELLED}
     * 상태에서는 변경이 불가능하다.</p>
     *
     * @throws BusinessException 주문이 이미 완료되었거나 취소된 경우
     */
    public void verifyModifiable() {
        if (this.status == StoreOrderStatus.COMPLETED || this.status == StoreOrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.STORE_ORDER_NOT_MODIFIABLE);
        }
    }
}
