package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreMenuCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreMenuUpdateRequest;
import com.dirtypay.domain.store.dto.response.StoreMenuResponse;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 매장 메뉴 비즈니스 로직 서비스.
 *
 * <p>매장 메뉴의 등록·수정·삭제·조회와 판매 가능 여부 토글 기능을 제공한다.
 * 쓰기 작업의 소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreMenuService {

    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;

    /**
     * 매장에 새로운 메뉴를 추가한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId 매장 ID
     * @param request 메뉴 생성 요청 DTO
     * @return 생성된 메뉴 응답 DTO
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     */
    @Transactional
    public StoreMenuResponse createMenu(Long storeId, StoreMenuCreateRequest request) {
        storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_NOT_FOUND));

        StoreMenu menu = StoreMenu.builder()
                .storeId(storeId)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .available(Boolean.TRUE.equals(request.getAvailable()))
                .sortOrder(request.getSortOrder())
                .build();

        return StoreMenuResponse.from(storeMenuRepository.save(menu));
    }

    /**
     * 매장 메뉴를 수정한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.
     * 요청 필드가 null이면 기존 값을 유지한다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @param request 메뉴 수정 요청 DTO
     * @return 수정된 메뉴 응답 DTO
     * @throws EntityNotFoundException 메뉴를 찾을 수 없는 경우
     */
    @Transactional
    public StoreMenuResponse updateMenu(Long storeId, Long menuId, StoreMenuUpdateRequest request) {
        StoreMenu menu = storeMenuRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND));

        menu.update(
                request.getName() != null ? request.getName() : menu.getName(),
                request.getDescription() != null ? request.getDescription() : menu.getDescription(),
                request.getPrice() != null ? request.getPrice() : menu.getPrice(),
                request.getCategory() != null ? request.getCategory() : menu.getCategory(),
                request.getImageUrl() != null ? request.getImageUrl() : menu.getImageUrl(),
                request.getSortOrder() != null ? request.getSortOrder() : menu.getSortOrder()
        );

        return StoreMenuResponse.from(menu);
    }

    /**
     * 매장 메뉴를 삭제(Soft Delete)한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @throws EntityNotFoundException 메뉴를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteMenu(Long storeId, Long menuId) {
        StoreMenu menu = storeMenuRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND));
        menu.delete();
    }

    /**
     * 메뉴 판매 가능 여부를 토글한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @return 토글 후 메뉴 응답 DTO
     * @throws EntityNotFoundException 메뉴를 찾을 수 없는 경우
     */
    @Transactional
    public StoreMenuResponse toggleAvailability(Long storeId, Long menuId) {
        StoreMenu menu = storeMenuRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND));
        menu.toggleAvailability();
        return StoreMenuResponse.from(menu);
    }

    /**
     * 매장의 전체 메뉴 목록을 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @return 메뉴 응답 DTO 목록
     */
    public List<StoreMenuResponse> getMenus(Long storeId) {
        return storeMenuRepository.findAllByStoreId(storeId).stream()
                .map(StoreMenuResponse::from)
                .toList();
    }

    /**
     * 특정 메뉴 상세 정보를 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @param menuId  메뉴 ID
     * @return 메뉴 응답 DTO
     * @throws EntityNotFoundException 메뉴를 찾을 수 없는 경우
     */
    public StoreMenuResponse getMenu(Long storeId, Long menuId) {
        StoreMenu menu = storeMenuRepository.findByIdAndStoreId(menuId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND));
        return StoreMenuResponse.from(menu);
    }

    /**
     * 매장의 판매 가능한 메뉴 목록을 노출 순서(sortOrder) 오름차순으로 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @return 판매 가능 메뉴 응답 DTO 목록 (sortOrder 오름차순)
     */
    public List<StoreMenuResponse> getAvailableMenus(Long storeId) {
        return storeMenuRepository.findAllByStoreIdAndAvailableOrderBySortOrder(storeId, true).stream()
                .map(StoreMenuResponse::from)
                .toList();
    }
}
