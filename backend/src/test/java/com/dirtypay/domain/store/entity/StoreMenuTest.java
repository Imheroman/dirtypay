package com.dirtypay.domain.store.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StoreMenu} Entity 단위 테스트.
 *
 * <p>메뉴 생성, 정보 수정, available 토글 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class StoreMenuTest {

    @Nested
    @DisplayName("StoreMenu 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("Builder로 StoreMenu 생성 시 모든 필드가 올바르게 설정된다")
        void StoreMenu_create_allFieldsSet() {
            // given & when
            StoreMenu menu = StoreMenu.builder()
                    .storeId(1L)
                    .name("삼겹살")
                    .description("국내산 삼겹살")
                    .price(new BigDecimal("15000"))
                    .category("육류")
                    .imageUrl("https://example.com/image.jpg")
                    .available(true)
                    .sortOrder(1)
                    .build();

            // then
            assertThat(menu.getStoreId()).isEqualTo(1L);
            assertThat(menu.getName()).isEqualTo("삼겹살");
            assertThat(menu.getDescription()).isEqualTo("국내산 삼겹살");
            assertThat(menu.getPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(menu.getCategory()).isEqualTo("육류");
            assertThat(menu.getImageUrl()).isEqualTo("https://example.com/image.jpg");
            assertThat(menu.isAvailable()).isTrue();
            assertThat(menu.getSortOrder()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("StoreMenu 정보 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("update 메서드 호출 시 메뉴 정보가 올바르게 변경된다")
        void StoreMenu_update_nameAndPrice() {
            // given
            StoreMenu menu = createMenu(1L, 1L, "삼겹살", new BigDecimal("15000"), true, 1);

            // when
            menu.update("소고기", "국내산 한우", new BigDecimal("35000"), "육류", null, 2);

            // then
            assertThat(menu.getName()).isEqualTo("소고기");
            assertThat(menu.getDescription()).isEqualTo("국내산 한우");
            assertThat(menu.getPrice()).isEqualByComparingTo(new BigDecimal("35000"));
            assertThat(menu.getCategory()).isEqualTo("육류");
            assertThat(menu.getImageUrl()).isNull();
            assertThat(menu.getSortOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("StoreMenu available 토글 테스트")
    class ToggleAvailabilityTest {

        @Test
        @DisplayName("available이 true인 경우 toggleAvailability 호출 후 false로 전환된다")
        void StoreMenu_toggleAvailability_success() {
            // given
            StoreMenu menu = createMenu(1L, 1L, "삼겹살", new BigDecimal("15000"), true, 1);
            assertThat(menu.isAvailable()).isTrue();

            // when
            menu.toggleAvailability();

            // then
            assertThat(menu.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("toggleAvailability를 두 번 호출하면 원래 값으로 돌아온다")
        void StoreMenu_toggleAvailability_multiple_times() {
            // given
            StoreMenu menu = createMenu(1L, 1L, "삼겹살", new BigDecimal("15000"), true, 1);

            // when
            menu.toggleAvailability();  // true -> false
            menu.toggleAvailability();  // false -> true

            // then
            assertThat(menu.isAvailable()).isTrue();
        }
    }

    // === Helper Methods ===

    private StoreMenu createMenu(Long id, Long storeId, String name, BigDecimal price,
                                  boolean available, int sortOrder) {
        StoreMenu menu = StoreMenu.builder()
                .storeId(storeId)
                .name(name)
                .price(price)
                .available(available)
                .sortOrder(sortOrder)
                .build();
        ReflectionTestUtils.setField(menu, "id", id);
        return menu;
    }
}
