package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreOrderCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreOrderStatusChangeRequest;
import com.dirtypay.domain.store.dto.response.StoreOrderResponse;
import com.dirtypay.domain.store.entity.StoreMenu;
import com.dirtypay.domain.store.entity.StoreOrder;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
import com.dirtypay.domain.store.repository.StoreMenuRepository;
import com.dirtypay.domain.store.repository.StoreOrderRepository;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 매장 주문 비즈니스 로직 서비스.
 *
 * <p>매장 주문의 생성·상태 변경·취소·조회 기능을 제공한다.
 * 주문 상태 변경(확인·완료·취소)의 소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreOrderService {

    private final StoreRepository storeRepository;
    private final StoreMenuRepository storeMenuRepository;
    private final StoreOrderRepository storeOrderRepository;

    /**
     * 매장에 새로운 주문을 생성한다.
     *
     * <p>매장 활성 상태와 메뉴 판매 가능 여부를 검증한 후 주문을 생성한다.
     * 주문 번호는 UUID로 자동 생성된다.</p>
     *
     * @param storeId 매장 ID
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 응답 DTO
     * @throws EntityNotFoundException 매장 또는 메뉴를 찾을 수 없는 경우
     * @throws BusinessException       매장 비활성화 또는 메뉴 판매 불가 상태인 경우
     */
    @Transactional
    public StoreOrderResponse createOrder(Long storeId, StoreOrderCreateRequest request) {
        storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_NOT_FOUND))
                .verifyActive();

        StoreMenu menu = storeMenuRepository.findByIdAndStoreId(request.getMenuId(), storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_MENU_NOT_FOUND));

        if (!menu.isAvailable()) {
            throw new BusinessException(ErrorCode.STORE_MENU_NOT_AVAILABLE);
        }

        BigDecimal totalPrice = menu.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        StoreOrder order = StoreOrder.builder()
                .storeId(storeId)
                .menuId(menu.getId())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .unitPrice(menu.getPrice())
                .menuName(menu.getName())
                .memberId(request.getMemberId())
                .orderNumber(UUID.randomUUID().toString())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .build();

        return StoreOrderResponse.from(storeOrderRepository.save(order));
    }

    /**
     * 매장 주문 상태를 변경한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.
     * 요청 상태에 따라 {@code confirm()}, {@code complete()}, {@code cancel()} 메서드를 호출한다.</p>
     *
     * @param storeId 매장 ID
     * @param orderId 주문 ID
     * @param request 주문 상태 변경 요청 DTO
     * @return 상태가 변경된 주문 응답 DTO
     * @throws EntityNotFoundException 주문을 찾을 수 없는 경우
     * @throws BusinessException       변경 불가능한 상태인 경우
     */
    @Transactional
    public StoreOrderResponse changeOrderStatus(Long storeId, Long orderId, StoreOrderStatusChangeRequest request) {
        StoreOrder order = storeOrderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_ORDER_NOT_FOUND));

        StoreOrderStatus targetStatus = request.getStatus();
        switch (targetStatus) {
            case CONFIRMED -> order.confirm();
            case COMPLETED -> order.complete();
            case CANCELLED -> order.cancel();
            default -> throw new BusinessException(ErrorCode.STORE_ORDER_NOT_MODIFIABLE);
        }

        return StoreOrderResponse.from(order);
    }

    /**
     * 매장 주문을 취소한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId 매장 ID
     * @param orderId 주문 ID
     * @throws EntityNotFoundException 주문을 찾을 수 없는 경우
     * @throws BusinessException       취소 불가능한 상태인 경우
     */
    @Transactional
    public void cancelOrder(Long storeId, Long orderId) {
        StoreOrder order = storeOrderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_ORDER_NOT_FOUND));
        order.cancel();
    }

    /**
     * 매장의 주문 목록을 페이지 단위로 최신순 조회한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId  매장 ID
     * @param pageable 페이지 요청 정보
     * @return 주문 응답 DTO 페이지 (최신순)
     */
    public Page<StoreOrderResponse> getOrders(Long storeId, Pageable pageable) {
        return storeOrderRepository.findAllByStoreIdOrderByCreatedDateDesc(storeId, pageable)
                .map(StoreOrderResponse::from);
    }

    /**
     * 매장 주문 상세 정보를 조회한다.
     *
     * <p>비로그인 사용자도 주문 번호를 알면 조회 가능하다.</p>
     *
     * @param storeId 매장 ID
     * @param orderId 주문 ID
     * @return 주문 응답 DTO
     * @throws EntityNotFoundException 주문을 찾을 수 없는 경우
     */
    public StoreOrderResponse getOrder(Long storeId, Long orderId) {
        StoreOrder order = storeOrderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_ORDER_NOT_FOUND));
        return StoreOrderResponse.from(order);
    }

    /**
     * 매장의 특정 상태 주문 목록을 페이지 단위로 최신순 조회한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId  매장 ID
     * @param status   조회할 주문 상태
     * @param pageable 페이지 요청 정보
     * @return 해당 상태의 주문 응답 DTO 페이지 (최신순)
     */
    public Page<StoreOrderResponse> getOrdersByStatus(Long storeId, StoreOrderStatus status, Pageable pageable) {
        return storeOrderRepository.findAllByStoreIdAndStatusOrderByCreatedDateDesc(storeId, status, pageable)
                .map(StoreOrderResponse::from);
    }
}
