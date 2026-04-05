package com.dirtypay.domain.group.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RoundGroupSharedMenu} 단위 테스트.
 *
 * <p>Builder 생성, 필드 getter, Soft Delete 동작을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class RoundGroupSharedMenuTest {

    @Test
    @DisplayName("Builder로 생성 시 groupId, menuId, quantity가 올바르게 설정된다")
    void createRoundGroupSharedMenu_allFieldsSet() {
        // given & when
        RoundGroupSharedMenu sharedMenu = RoundGroupSharedMenu.builder()
                .groupId(10L)
                .menuId(50L)
                .quantity(3)
                .build();

        // then
        assertThat(sharedMenu.getGroupId()).isEqualTo(10L);
        assertThat(sharedMenu.getMenuId()).isEqualTo(50L);
        assertThat(sharedMenu.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("delete() 호출 시 isDeleted()가 true로 변경된다 (BaseEntity Soft Delete)")
    void delete_softDeletesTheSharedMenu() {
        // given
        RoundGroupSharedMenu sharedMenu = RoundGroupSharedMenu.builder()
                .groupId(10L)
                .menuId(50L)
                .quantity(2)
                .build();

        assertThat(sharedMenu.isDeleted()).isFalse();

        // when
        sharedMenu.delete();

        // then
        assertThat(sharedMenu.isDeleted()).isTrue();
        assertThat(sharedMenu.getDeletedDate()).isNotNull();
    }
}
