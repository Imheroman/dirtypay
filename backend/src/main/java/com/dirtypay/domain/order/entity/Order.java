package com.dirtypay.domain.order.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주문 엔티티.
 *
 * <p>라운드 내에서 특정 메뉴에 대한 주문을 나타낸다.
 * totalPrice는 menu.price * quantity로 계산된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class Order extends BaseEntity {

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Column(name = "menu_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal menuPrice;

    @Column(name = "menu_category")
    private String menuCategory;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Builder
    public Order(Long roundId, Long groupId, String groupName, Long menuId, String menuName,
                 BigDecimal menuPrice, String menuCategory, int quantity, BigDecimal totalPrice) {
        this.roundId = roundId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.menuId = menuId;
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.menuCategory = menuCategory;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    /**
     * 주문 수량을 변경하고 스냅샷된 menuPrice로 총 가격을 재계산한다.
     *
     * @param quantity 변경할 수량
     */
    public void updateQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPrice = this.menuPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
