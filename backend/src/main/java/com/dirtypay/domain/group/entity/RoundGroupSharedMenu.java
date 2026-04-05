package com.dirtypay.domain.group.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 라운드 그룹 공유 메뉴 엔티티.
 *
 * <p>그룹에 등록된 공유 메뉴와 수량을 나타낸다.
 * 공유 메뉴는 그룹 전체가 함께 부담하는 메뉴이다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "round_group_shared_menus", indexes = {
        @Index(name = "idx_round_group_shared_menus_group", columnList = "group_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class RoundGroupSharedMenu extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(nullable = false)
    private int quantity;

    @Builder
    public RoundGroupSharedMenu(Long groupId, Long menuId, int quantity) {
        this.groupId = groupId;
        this.menuId = menuId;
        this.quantity = quantity;
    }
}
