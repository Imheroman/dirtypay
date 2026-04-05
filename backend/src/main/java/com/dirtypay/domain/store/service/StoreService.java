package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreStatusChangeRequest;
import com.dirtypay.domain.store.dto.request.StoreUpdateRequest;
import com.dirtypay.domain.store.dto.response.PopularMenuResponse;
import com.dirtypay.domain.store.dto.response.StoreResponse;
import com.dirtypay.domain.store.dto.response.StoreStatisticsResponse;
import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.domain.store.repository.PopularMenuProjection;
import com.dirtypay.domain.store.repository.StoreOrderRepository;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.domain.store.strategy.StoreRegistrationStrategy;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 매장 비즈니스 로직 서비스.
 *
 * <p>매장 등록·수정·삭제·조회와 통계 기능을 제공한다.
 * 등록 방식(직접/POS 연동)은 {@link StoreRegistrationStrategy} 전략 패턴으로 분기한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    /** 통계 집계 대상 주문 상태: CONFIRMED, COMPLETED. */
    private static final List<StoreOrderStatus> COUNTABLE_STATUSES =
            List.of(StoreOrderStatus.CONFIRMED, StoreOrderStatus.COMPLETED);

    private final StoreRepository storeRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final List<StoreRegistrationStrategy> strategies;

    /**
     * 새로운 매장을 등록한다.
     *
     * <p>사업자 등록번호 중복 검사(애플리케이션 레벨) → 전략 선택 → 전략 유효성 검증 →
     * 엔티티 저장 → 전략 후처리 순서로 진행한다.</p>
     *
     * <p>애플리케이션 레벨 중복 검사를 통과하더라도 동시 요청으로 인한 Race Condition이 발생할 수 있다.
     * DB 레벨 UNIQUE 제약({@code uk_business_number})이 최후 방어선이며,
     * {@link DataIntegrityViolationException}을 {@link ErrorCode#STORE_BUSINESS_NUMBER_DUPLICATED}로 변환한다.</p>
     *
     * @param ownerId 매장 소유자 회원 ID
     * @param request 매장 등록 요청 DTO
     * @return 등록된 매장 응답 DTO
     * @throws BusinessException 사업자 등록번호 중복 또는 지원하지 않는 타입인 경우
     */
    @Transactional
    public StoreResponse createStore(Long ownerId, StoreCreateRequest request) {
        if (request.getBusinessNumber() != null
                && storeRepository.findByBusinessNumber(request.getBusinessNumber()).isPresent()) {
            throw new BusinessException(ErrorCode.STORE_BUSINESS_NUMBER_DUPLICATED);
        }

        StoreRegistrationStrategy strategy = strategies.stream()
                .filter(s -> s.supports().equals(request.getStoreType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_TYPE_NOT_SUPPORTED));

        strategy.validate(request.getStoreType(), request.getPosIntegrationKey());

        Store store = Store.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .businessNumber(request.getBusinessNumber())
                .address(request.getAddress())
                .phone(request.getPhone())
                .description(request.getDescription())
                .storeType(request.getStoreType())
                .status(StoreStatus.ACTIVE)
                .posIntegrationKey(request.getPosIntegrationKey())
                .build();

        try {
            Store saved = storeRepository.saveAndFlush(store);
            strategy.onRegister(saved);
            return StoreResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.STORE_BUSINESS_NUMBER_DUPLICATED);
        }
    }

    /**
     * 매장 정보를 수정한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId 매장 ID
     * @param request 매장 수정 요청 DTO
     * @return 수정된 매장 응답 DTO
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     */
    @Transactional
    public StoreResponse updateStore(Long storeId, StoreUpdateRequest request) {
        Store store = findStoreById(storeId);
        store.update(
                request.getName() != null ? request.getName() : store.getName(),
                request.getAddress() != null ? request.getAddress() : store.getAddress(),
                request.getPhone() != null ? request.getPhone() : store.getPhone(),
                request.getDescription() != null ? request.getDescription() : store.getDescription()
        );
        return StoreResponse.from(store);
    }

    /**
     * 매장 운영 상태를 변경한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId 매장 ID
     * @param request 상태 변경 요청 DTO
     * @return 상태가 변경된 매장 응답 DTO
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     * @throws BusinessException       이미 폐업한 매장인 경우
     */
    @Transactional
    public StoreResponse changeStatus(Long storeId, StoreStatusChangeRequest request) {
        Store store = findStoreById(storeId);
        store.changeStatus(request.getStatus());
        return StoreResponse.from(store);
    }

    /**
     * 매장을 삭제(Soft Delete)한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId 매장 ID
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     */
    @Transactional
    public void deleteStore(Long storeId) {
        Store store = findStoreById(storeId);
        store.delete();
    }

    /**
     * 매장 상세 정보를 조회한다. CUSTOM 매장은 소유자만 접근 가능하다.
     *
     * <p>{@link StoreType#CUSTOM} 매장을 비소유자 또는 비로그인 사용자가 조회하면
     * {@link ErrorCode#STORE_CUSTOM_ACCESS_DENIED} 예외가 발생한다.</p>
     *
     * @param storeId     매장 ID
     * @param requesterId 요청자 회원 ID (비로그인 시 null)
     * @return 매장 응답 DTO
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     * @throws BusinessException       CUSTOM 매장에 비소유자가 접근하는 경우
     */
    public StoreResponse getStore(Long storeId, Long requesterId) {
        Store store = findStoreById(storeId);
        if (StoreType.CUSTOM.equals(store.getStoreType())
                && !store.getOwnerId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.STORE_CUSTOM_ACCESS_DENIED);
        }
        return StoreResponse.from(store);
    }

    /**
     * 매장 목록을 페이지 단위로 조회한다.
     *
     * <p>비로그인 사용자도 조회 가능하다.
     * {@link StoreType#CUSTOM} 매장은 제외하고 {@link StoreStatus#ACTIVE}인 매장만 반환한다.</p>
     *
     * @param pageable 페이지 요청 정보
     * @return 매장 응답 DTO 페이지
     */
    public Page<StoreResponse> getStores(Pageable pageable) {
        return storeRepository.findAllByStoreTypeNotAndStatus(
                        StoreType.CUSTOM, StoreStatus.ACTIVE, pageable)
                .map(StoreResponse::from);
    }

    /**
     * 내 매장 목록을 페이지 단위로 조회한다.
     *
     * <p>소유자의 전체 매장(상태 무관)을 반환한다.</p>
     *
     * @param ownerId  매장 소유자 회원 ID
     * @param pageable 페이지 요청 정보
     * @return 소유자의 매장 응답 DTO 페이지
     */
    public Page<StoreResponse> getMyStores(Long ownerId, Pageable pageable) {
        return storeRepository.findAllByOwnerId(ownerId, pageable)
                .map(StoreResponse::from);
    }

    /**
     * 매장 통계(주문 건수, 매출액, 일 평균 주문 수)를 조회한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId   매장 ID
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 매장 통계 응답 DTO
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     */
    public StoreStatisticsResponse getStatistics(Long storeId, LocalDate startDate, LocalDate endDate) {
        findStoreById(storeId);

        long totalOrders = storeOrderRepository.countByStoreIdAndPeriod(
                storeId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX), COUNTABLE_STATUSES);
        BigDecimal totalRevenue = storeOrderRepository.sumTotalPriceByStoreIdAndPeriod(
                storeId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX), COUNTABLE_STATUSES);

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal averageDailyOrders = days > 0
                ? BigDecimal.valueOf(totalOrders).divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return StoreStatisticsResponse.builder()
                .storeId(storeId)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageDailyOrders(averageDailyOrders)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    /**
     * 기간 내 인기 메뉴 목록(주문 건수 기준)을 조회한다.
     *
     * <p>소유권 검증은 Controller 레이어의 {@code @StoreOwner} AOP에서 수행된다.</p>
     *
     * @param storeId   매장 ID
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 인기 메뉴 목록 응답 DTO
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     */
    public PopularMenuResponse getPopularMenus(Long storeId, LocalDate startDate, LocalDate endDate) {
        findStoreById(storeId);

        List<PopularMenuProjection> results = storeOrderRepository.findPopularMenusByPeriod(
                storeId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        List<PopularMenuResponse.MenuItem> items = results.stream()
                .map(row -> PopularMenuResponse.MenuItem.builder()
                        .menuId(row.getMenuId())
                        .menuName(row.getMenuName())
                        .orderCount(row.getOrderCount())
                        .revenue(row.getRevenue())
                        .build())
                .toList();

        return PopularMenuResponse.builder()
                .menus(items)
                .build();
    }

    /**
     * ID로 매장 엔티티를 조회하는 내부 헬퍼.
     *
     * <p>서비스 내 여러 메서드에서 공통으로 사용하는 단순 조회 로직을 캡슐화한다.
     * 접근 제어가 필요한 외부 노출은 {@link #getStore(Long, Long)}을 사용한다.</p>
     *
     * @param storeId 매장 ID
     * @return 조회된 {@link Store} 엔티티
     * @throws EntityNotFoundException 매장을 찾을 수 없는 경우
     */
    private Store findStoreById(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STORE_NOT_FOUND));
    }

}
