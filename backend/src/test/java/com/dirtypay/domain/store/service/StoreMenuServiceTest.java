package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreMenuCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreMenuUpdateRequest;
import com.dirtypay.domain.store.dto.response.StoreMenuResponse;
import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link StoreMenuService} 단위 테스트.
 *
 * <p>메뉴 등록, 수정, 삭제, 조회, available 토글 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StoreMenuServiceTest {

    @InjectMocks
    private StoreMenuService storeMenuService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMenuRepository storeMenuRepository;

    @Nested
    @DisplayName("메뉴 등록 테스트")
    class CreateMenuTest {

        @Test
        @DisplayName("유효한 storeId로 메뉴 등록 성공 시 StoreMenuResponse를 반환한다")
        void StoreMenuService_createMenu_success() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId, 1L, "테스트 매장");
            StoreMenuCreateRequest request = createMenuRequest("삼겹살", new BigDecimal("15000"), true, 1);
            StoreMenu savedMenu = createMenu(10L, storeId, "삼겹살", new BigDecimal("15000"), true, 1);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeMenuRepository.save(any(StoreMenu.class))).willReturn(savedMenu);

            // when
            StoreMenuResponse response = storeMenuService.createMenu(storeId, request);

            // then
            assertThat(response.getName()).isEqualTo("삼겹살");
            assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(response.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 storeId로 메뉴 등록 시 EntityNotFoundException이 발생한다")
        void StoreMenuService_createMenu_throwsWhenStoreNotFound() {
            // given
            Long nonExistentStoreId = 999L;
            StoreMenuCreateRequest request = createMenuRequest("삼겹살", new BigDecimal("15000"), true, 1);

            given(storeRepository.findById(nonExistentStoreId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeMenuService.createMenu(nonExistentStoreId, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("메뉴 수정 테스트")
    class UpdateMenuTest {

        @Test
        @DisplayName("유효한 menuId + storeId로 메뉴 수정 성공 시 변경된 StoreMenuResponse를 반환한다")
        void StoreMenuService_updateMenu_success() {
            // given
            Long storeId = 1L;
            Long menuId = 10L;
            StoreMenu menu = createMenu(menuId, storeId, "삼겹살", new BigDecimal("15000"), true, 1);
            StoreMenuUpdateRequest request = createUpdateRequest("소고기", new BigDecimal("35000"));

            given(storeMenuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));

            // when
            StoreMenuResponse response = storeMenuService.updateMenu(storeId, menuId, request);

            // then
            assertThat(response.getName()).isEqualTo("소고기");
            assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("35000"));
        }

        @Test
        @DisplayName("존재하지 않는 menuId로 메뉴 수정 시 EntityNotFoundException이 발생한다")
        void StoreMenuService_updateMenu_notFound_throwsException() {
            // given
            Long storeId = 1L;
            Long nonExistentMenuId = 999L;
            StoreMenuUpdateRequest request = createUpdateRequest("소고기", new BigDecimal("35000"));

            given(storeMenuRepository.findByIdAndStoreId(nonExistentMenuId, storeId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeMenuService.updateMenu(storeId, nonExistentMenuId, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("메뉴 available 토글 테스트")
    class ToggleAvailabilityTest {

        @Test
        @DisplayName("available 토글 성공 시 변경된 상태의 StoreMenuResponse를 반환한다")
        void StoreMenuService_toggleAvailability_success() {
            // given
            Long storeId = 1L;
            Long menuId = 10L;
            StoreMenu menu = createMenu(menuId, storeId, "삼겹살", new BigDecimal("15000"), true, 1);

            given(storeMenuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));

            // when
            StoreMenuResponse response = storeMenuService.toggleAvailability(storeId, menuId);

            // then — true -> false 토글
            assertThat(response.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("메뉴 삭제 테스트")
    class DeleteMenuTest {

        @Test
        @DisplayName("메뉴 Soft Delete 성공 시 deletedDate가 설정된다")
        void StoreMenuService_deleteMenu_success() {
            // given
            Long storeId = 1L;
            Long menuId = 10L;
            StoreMenu menu = createMenu(menuId, storeId, "삼겹살", new BigDecimal("15000"), true, 1);

            given(storeMenuRepository.findByIdAndStoreId(menuId, storeId)).willReturn(Optional.of(menu));

            // when
            storeMenuService.deleteMenu(storeId, menuId);

            // then
            assertThat(menu.getDeletedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("메뉴 조회 테스트")
    class GetMenusTest {

        @Test
        @DisplayName("getMenus: storeId로 전체 메뉴 목록을 반환한다")
        void StoreMenuService_getMenusByStoreId_returnsMenus() {
            // given
            Long storeId = 1L;
            List<StoreMenu> menus = List.of(
                    createMenu(1L, storeId, "삼겹살", new BigDecimal("15000"), true, 1),
                    createMenu(2L, storeId, "소주", new BigDecimal("5000"), true, 2)
            );

            given(storeMenuRepository.findAllByStoreId(storeId)).willReturn(menus);

            // when
            List<StoreMenuResponse> responses = storeMenuService.getMenus(storeId);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(StoreMenuResponse::getName)
                    .containsExactlyInAnyOrder("삼겹살", "소주");
        }
    }

    // === Helper Methods ===

    private Store createStore(Long id, Long ownerId, String name) {
        Store store = Store.builder()
                .ownerId(ownerId)
                .name(name)
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .storeType(StoreType.DIRECT)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

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

    private StoreMenuCreateRequest createMenuRequest(String name, BigDecimal price,
                                                      boolean available, int sortOrder) {
        StoreMenuCreateRequest request = new StoreMenuCreateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "price", price);
        ReflectionTestUtils.setField(request, "available", available);
        ReflectionTestUtils.setField(request, "sortOrder", sortOrder);
        return request;
    }

    private StoreMenuUpdateRequest createUpdateRequest(String name, BigDecimal price) {
        StoreMenuUpdateRequest request = new StoreMenuUpdateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "price", price);
        return request;
    }
}
