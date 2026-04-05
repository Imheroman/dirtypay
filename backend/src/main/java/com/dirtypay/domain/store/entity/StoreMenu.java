package com.dirtypay.domain.store.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 매장 메뉴 엔티티.
 *
 * <p>특정 매장({@code storeId})에 속하는 메뉴 항목을 나타낸다.
 * {@code available} 플래그로 판매 가능 여부를 제어하며,
 * {@code sortOrder}로 메뉴 노출 순서를 관리한다.</p>
 *
 * <p>Soft Delete: {@code deleted_date IS NULL} 조건으로 삭제된 레코드를
 * 자동 제외한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "store_menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class StoreMenu extends BaseEntity {

    /**
     * 소속 매장 ID. FK → stores.
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     * 메뉴명. 최대 100자.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 메뉴 설명. 최대 500자. 선택 항목.
     */
    @Column(length = 500)
    private String description;

    /**
     * 메뉴 가격.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * 메뉴 카테고리. 최대 50자. 선택 항목.
     */
    @Column(length = 50)
    private String category;

    /**
     * 메뉴 이미지 URL. 최대 500자. 선택 항목.
     */
    @Column(length = 500)
    private String imageUrl;

    /**
     * 판매 가능 여부. {@code true}이면 주문 가능.
     */
    @Column(nullable = false)
    private boolean available;

    /**
     * 메뉴 노출 순서. 낮을수록 먼저 노출.
     */
    @Column(nullable = false)
    private int sortOrder;

    /**
     * StoreMenu 엔티티 생성자.
     *
     * @param storeId     소속 매장 ID
     * @param name        메뉴명
     * @param description 메뉴 설명
     * @param price       가격
     * @param category    카테고리
     * @param imageUrl    이미지 URL
     * @param available   판매 가능 여부
     * @param sortOrder   노출 순서
     */
    @Builder
    public StoreMenu(Long storeId, String name, String description, BigDecimal price,
                     String category, String imageUrl, boolean available, int sortOrder) {
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.available = available;
        this.sortOrder = sortOrder;
    }

    /**
     * 메뉴 정보를 수정한다.
     *
     * @param name        변경할 메뉴명
     * @param description 변경할 설명
     * @param price       변경할 가격
     * @param category    변경할 카테고리
     * @param imageUrl    변경할 이미지 URL
     * @param sortOrder   변경할 노출 순서
     */
    public void update(String name, String description, BigDecimal price,
                       String category, String imageUrl, int sortOrder) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    /**
     * 판매 가능 여부를 토글한다.
     *
     * <p>{@code available}이 {@code true}이면 {@code false}로, 반대도 동일하게 변경한다.</p>
     */
    public void toggleAvailability() {
        this.available = !this.available;
    }
}
