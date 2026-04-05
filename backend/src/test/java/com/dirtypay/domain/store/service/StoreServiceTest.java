package com.dirtypay.domain.store.service;

import com.dirtypay.domain.store.dto.request.StoreCreateRequest;
import com.dirtypay.domain.store.dto.request.StoreStatusChangeRequest;
import com.dirtypay.domain.store.dto.request.StoreUpdateRequest;
import com.dirtypay.domain.store.dto.response.StoreResponse;
import com.dirtypay.domain.store.dto.response.StoreStatisticsResponse;
import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreOrderStatus;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.domain.store.repository.StoreOrderRepository;
import com.dirtypay.domain.store.repository.StoreRepository;
import com.dirtypay.domain.store.strategy.CustomRegistrationStrategy;
import com.dirtypay.domain.store.strategy.DirectRegistrationStrategy;
import com.dirtypay.domain.store.strategy.PosIntegrationStrategy;
import com.dirtypay.domain.store.strategy.StoreRegistrationStrategy;
import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.exception.BusinessException;
import com.dirtypay.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link StoreService} 단위 테스트.
 *
 * <p>매장 등록(전략 패턴), 수정, 삭제, 조회, 상태 변경 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreOrderRepository storeOrderRepository;

    @Mock
    private DirectRegistrationStrategy directStrategy;

    @Mock
    private PosIntegrationStrategy posStrategy;

    @Mock
    private CustomRegistrationStrategy customStrategy;

    private List<StoreRegistrationStrategy> strategies;

    @BeforeEach
    void setUp() {
        // List<StoreRegistrationStrategy>를 @InjectMocks 대상에 직접 주입
        strategies = List.of(directStrategy, posStrategy, customStrategy);
        ReflectionTestUtils.setField(storeService, "strategies", strategies);
    }

    @Nested
    @DisplayName("매장 등록 테스트")
    class CreateStoreTest {

        @Test
        @DisplayName("DIRECT 타입으로 매장 등록 성공 시 StoreResponse를 반환한다")
        void StoreService_createStore_successWithDirectRegistration() {
            // given
            StoreCreateRequest request = createStoreRequest("테스트 매장", "123-45-67890", StoreType.DIRECT, null);
            Store savedStore = createStore(1L, 1L, "테스트 매장", StoreType.DIRECT);

            given(storeRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.empty());
            given(directStrategy.supports()).willReturn(StoreType.DIRECT);
            given(storeRepository.saveAndFlush(any(Store.class))).willReturn(savedStore);

            // when
            StoreResponse response = storeService.createStore(1L, request);

            // then
            assertThat(response.getName()).isEqualTo("테스트 매장");
            assertThat(response.getStoreType()).isEqualTo(StoreType.DIRECT);
            assertThat(response.getStatus()).isEqualTo(StoreStatus.ACTIVE);
            verify(directStrategy).validate(StoreType.DIRECT, null);
            verify(directStrategy).onRegister(savedStore);
        }

        @Test
        @DisplayName("POS_INTEGRATED 타입으로 매장 등록 성공 시 StoreResponse를 반환한다")
        void StoreService_createStore_successWithPosRegistration() {
            // given
            StoreCreateRequest request = createStoreRequest("POS 매장", "222-22-22222",
                    StoreType.POS_INTEGRATED, "valid-pos-key");
            Store savedStore = createStore(2L, 1L, "POS 매장", StoreType.POS_INTEGRATED);

            given(storeRepository.findByBusinessNumber("222-22-22222")).willReturn(Optional.empty());
            given(directStrategy.supports()).willReturn(StoreType.DIRECT);
            given(posStrategy.supports()).willReturn(StoreType.POS_INTEGRATED);
            given(storeRepository.saveAndFlush(any(Store.class))).willReturn(savedStore);

            // when
            StoreResponse response = storeService.createStore(1L, request);

            // then
            assertThat(response.getStoreType()).isEqualTo(StoreType.POS_INTEGRATED);
            verify(posStrategy).validate(StoreType.POS_INTEGRATED, "valid-pos-key");
        }

        @Test
        @DisplayName("중복된 사업자번호 등록 시 BusinessException이 발생한다")
        void StoreService_createStore_duplicateBusinessNumber_throwsException() {
            // given
            StoreCreateRequest request = createStoreRequest("테스트 매장", "111-11-11111",
                    StoreType.DIRECT, null);
            Store existingStore = createStore(1L, 99L, "기존 매장", StoreType.DIRECT);

            given(storeRepository.findByBusinessNumber("111-11-11111")).willReturn(Optional.of(existingStore));

            // when & then
            assertThatThrownBy(() -> storeService.createStore(1L, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("매장 조회 테스트")
    class GetStoreTest {

        @Test
        @DisplayName("존재하는 storeId 조회 시 StoreResponse를 반환한다")
        void StoreService_getStore_success() {
            // given
            Long storeId = 1L;
            Long ownerId = 1L;
            Store store = createStore(storeId, ownerId, "테스트 매장", StoreType.DIRECT);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            StoreResponse response = storeService.getStore(storeId, ownerId);

            // then
            assertThat(response.getId()).isEqualTo(storeId);
            assertThat(response.getName()).isEqualTo("테스트 매장");
        }

        @Test
        @DisplayName("존재하지 않는 storeId 조회 시 EntityNotFoundException이 발생한다")
        void StoreService_getStore_notFound_throwsException() {
            // given
            given(storeRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> storeService.getStore(999L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("매장 수정 테스트")
    class UpdateStoreTest {

        @Test
        @DisplayName("매장 정보 수정 성공 시 변경된 StoreResponse를 반환한다")
        void StoreService_updateStore_success() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId, 1L, "원래 매장명", StoreType.DIRECT);
            StoreUpdateRequest request = createUpdateRequest("변경된 매장명", "변경된 주소", "010-9999-8888", "변경된 소개");

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            StoreResponse response = storeService.updateStore(storeId, request);

            // then
            assertThat(response.getName()).isEqualTo("변경된 매장명");
            assertThat(response.getAddress()).isEqualTo("변경된 주소");
        }
    }

    @Nested
    @DisplayName("매장 상태 변경 테스트")
    class ChangeStatusTest {

        @Test
        @DisplayName("ACTIVE 매장을 INACTIVE로 상태 변경 성공 시 변경된 StoreResponse를 반환한다")
        void StoreService_changeStatus_success() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId, 1L, "테스트 매장", StoreType.DIRECT);
            StoreStatusChangeRequest request = createStatusChangeRequest(StoreStatus.INACTIVE);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            StoreResponse response = storeService.changeStatus(storeId, request);

            // then
            assertThat(response.getStatus()).isEqualTo(StoreStatus.INACTIVE);
        }
    }

    @Nested
    @DisplayName("매장 소프트 삭제 테스트")
    class DeleteStoreTest {

        @Test
        @DisplayName("Soft Delete 성공 시 매장의 deletedDate가 설정된다")
        void StoreService_deleteStore_softDelete() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId, 1L, "테스트 매장", StoreType.DIRECT);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            storeService.deleteStore(storeId);

            // then — delete() 호출 후 deletedDate가 설정되는지 확인
            assertThat(store.getDeletedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("CUSTOM 매장 접근 제어 테스트")
    class GetStoreCustomAccessTest {

        @Test
        @DisplayName("CUSTOM 매장을 소유자가 조회하면 성공한다")
        void getStore_customStore_ownerAccess_success() {
            // given
            Long storeId = 1L;
            Long ownerId = 100L;
            Store store = Store.builder()
                    .ownerId(ownerId)
                    .name("나만의 가게")
                    .address("서울")
                    .storeType(StoreType.CUSTOM)
                    .status(StoreStatus.ACTIVE)
                    .build();
            ReflectionTestUtils.setField(store, "id", storeId);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            StoreResponse response = storeService.getStore(storeId, ownerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("나만의 가게");
        }

        @Test
        @DisplayName("CUSTOM 매장을 비소유자가 조회하면 STORE_CUSTOM_ACCESS_DENIED 예외가 발생한다")
        void getStore_customStore_nonOwnerAccess_throwsException() {
            // given
            Long storeId = 1L;
            Long ownerId = 100L;
            Long otherUserId = 200L;
            Store store = Store.builder()
                    .ownerId(ownerId)
                    .name("나만의 가게")
                    .address("서울")
                    .storeType(StoreType.CUSTOM)
                    .status(StoreStatus.ACTIVE)
                    .build();
            ReflectionTestUtils.setField(store, "id", storeId);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.getStore(storeId, otherUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_CUSTOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("CUSTOM 매장을 비로그인 사용자(null)가 조회하면 예외가 발생한다")
        void getStore_customStore_anonymousAccess_throwsException() {
            // given
            Long storeId = 1L;
            Long ownerId = 100L;
            Store store = Store.builder()
                    .ownerId(ownerId)
                    .name("나만의 가게")
                    .address("서울")
                    .storeType(StoreType.CUSTOM)
                    .status(StoreStatus.ACTIVE)
                    .build();
            ReflectionTestUtils.setField(store, "id", storeId);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when & then
            assertThatThrownBy(() -> storeService.getStore(storeId, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_CUSTOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("DIRECT 매장은 비로그인 사용자도 조회할 수 있다")
        void getStore_directStore_anonymousAccess_success() {
            // given
            Long storeId = 1L;
            Store store = Store.builder()
                    .ownerId(100L)
                    .name("일반 매장")
                    .address("서울")
                    .storeType(StoreType.DIRECT)
                    .status(StoreStatus.ACTIVE)
                    .build();
            ReflectionTestUtils.setField(store, "id", storeId);
            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));

            // when
            StoreResponse response = storeService.getStore(storeId, null);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("일반 매장");
        }
    }

    @Nested
    @DisplayName("매장 통계 조회 테스트 — averageDailyOrders BigDecimal 정밀도 검증")
    class GetStatisticsTest {

        /**
         * 7건 주문 / 3일 구간 → 일 평균 = 7 ÷ 3 = 2.33 (HALF_UP, 소수점 2자리).
         *
         * <p>double 연산(2.3333...)과 달리 BigDecimal.divide() 결과가
         * 정확히 2.33임을 검증한다.</p>
         */
        @Test
        @DisplayName("7건/3일 조회 시 averageDailyOrders가 2.33(BigDecimal)이다")
        void getStatistics_sevenOrdersOverThreeDays_averageIsCorrect() {
            // given
            Long storeId = 1L;
            Store store = createStore(storeId, 1L, "테스트 매장", StoreType.DIRECT);
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate   = LocalDate.of(2025, 1, 3); // 3일 구간

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeOrderRepository.countByStoreIdAndPeriod(
                    eq(storeId),
                    eq(startDate.atStartOfDay()),
                    any(LocalDateTime.class),
                    anyList()))
                    .willReturn(7L);
            given(storeOrderRepository.sumTotalPriceByStoreIdAndPeriod(
                    eq(storeId),
                    eq(startDate.atStartOfDay()),
                    any(LocalDateTime.class),
                    anyList()))
                    .willReturn(new BigDecimal("70000"));

            // when
            StoreStatisticsResponse response = storeService.getStatistics(storeId, startDate, endDate);

            // then — 7 / 3 = 2.33 (HALF_UP, scale=2)
            assertThat(response.getAverageDailyOrders())
                    .isEqualByComparingTo(new BigDecimal("2.33"));
            assertThat(response.getTotalOrders()).isEqualTo(7L);
            assertThat(response.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("70000"));
        }

        /**
         * 조회 기간이 0일인 경계 케이스: days = ChronoUnit.DAYS.between(same, same) + 1 = 1 이므로
         * 단일 날짜(1일) 구간에서 0건이면 averageDailyOrders = 0.00.
         */
        @Test
        @DisplayName("주문이 0건일 때 averageDailyOrders는 BigDecimal.ZERO이다")
        void getStatistics_zeroOrders_averageIsZero() {
            // given
            Long storeId = 2L;
            Store store = createStore(storeId, 1L, "빈 매장", StoreType.DIRECT);
            LocalDate date = LocalDate.of(2025, 1, 1);

            given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
            given(storeOrderRepository.countByStoreIdAndPeriod(
                    eq(storeId),
                    eq(date.atStartOfDay()),
                    any(LocalDateTime.class),
                    anyList()))
                    .willReturn(0L);
            given(storeOrderRepository.sumTotalPriceByStoreIdAndPeriod(
                    eq(storeId),
                    eq(date.atStartOfDay()),
                    any(LocalDateTime.class),
                    anyList()))
                    .willReturn(BigDecimal.ZERO);

            // when
            StoreStatisticsResponse response = storeService.getStatistics(storeId, date, date);

            // then
            assertThat(response.getAverageDailyOrders())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // === Helper Methods ===

    private Store createStore(Long id, Long ownerId, String name, StoreType storeType) {
        Store store = Store.builder()
                .ownerId(ownerId)
                .name(name)
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .storeType(storeType)
                .status(StoreStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

    private Store createStore(Long id, Long ownerId, String name, StoreType storeType, StoreStatus status) {
        Store store = Store.builder()
                .ownerId(ownerId)
                .name(name)
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .storeType(storeType)
                .status(status)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

    private StoreCreateRequest createStoreRequest(String name, String businessNumber,
                                                   StoreType storeType, String posKey) {
        StoreCreateRequest request = new StoreCreateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "businessNumber", businessNumber);
        ReflectionTestUtils.setField(request, "address", "서울시 강남구");
        ReflectionTestUtils.setField(request, "storeType", storeType);
        ReflectionTestUtils.setField(request, "posIntegrationKey", posKey);
        return request;
    }

    private StoreUpdateRequest createUpdateRequest(String name, String address,
                                                    String phone, String description) {
        StoreUpdateRequest request = new StoreUpdateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "address", address);
        ReflectionTestUtils.setField(request, "phone", phone);
        ReflectionTestUtils.setField(request, "description", description);
        return request;
    }

    private StoreStatusChangeRequest createStatusChangeRequest(StoreStatus status) {
        StoreStatusChangeRequest request = new StoreStatusChangeRequest();
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }
}
