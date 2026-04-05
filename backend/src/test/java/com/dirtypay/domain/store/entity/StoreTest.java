package com.dirtypay.domain.store.entity;

import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Store} Entity 단위 테스트.
 *
 * <p>Store 상태 전이(changeStatus, close, verifyActive)와 update 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class StoreTest {

    @Nested
    @DisplayName("Store 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("Builder로 Store 생성 시 모든 필드가 올바르게 설정되고 status를 ACTIVE로 지정할 수 있다")
        void Store_create_allFieldsSet() {
            // given & when
            Store store = Store.builder()
                    .ownerId(1L)
                    .name("테스트 매장")
                    .businessNumber("123-45-67890")
                    .address("서울시 강남구 테스트로 1")
                    .phone("010-1234-5678")
                    .description("테스트 매장 소개")
                    .storeType(StoreType.DIRECT)
                    .status(StoreStatus.ACTIVE)
                    .build();

            // then
            assertThat(store.getOwnerId()).isEqualTo(1L);
            assertThat(store.getName()).isEqualTo("테스트 매장");
            assertThat(store.getBusinessNumber()).isEqualTo("123-45-67890");
            assertThat(store.getAddress()).isEqualTo("서울시 강남구 테스트로 1");
            assertThat(store.getPhone()).isEqualTo("010-1234-5678");
            assertThat(store.getDescription()).isEqualTo("테스트 매장 소개");
            assertThat(store.getStoreType()).isEqualTo(StoreType.DIRECT);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Store 정보 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("update 메서드 호출 시 name, address, phone, description이 변경된다")
        void Store_update_successWithPartialFields() {
            // given
            Store store = createStore(1L, 1L, "원래 매장명", StoreType.DIRECT);

            // when
            store.update("변경된 매장명", "변경된 주소", "010-9999-8888", "변경된 소개");

            // then
            assertThat(store.getName()).isEqualTo("변경된 매장명");
            assertThat(store.getAddress()).isEqualTo("변경된 주소");
            assertThat(store.getPhone()).isEqualTo("010-9999-8888");
            assertThat(store.getDescription()).isEqualTo("변경된 소개");
        }
    }

    @Nested
    @DisplayName("Store 상태 변경 테스트")
    class ChangeStatusTest {

        @Test
        @DisplayName("ACTIVE 상태에서 INACTIVE로 상태 변경이 성공한다")
        void Store_changeStatus_activeToInactive() {
            // given
            Store store = createStore(1L, 1L, "테스트 매장", StoreType.DIRECT);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);

            // when
            store.changeStatus(StoreStatus.INACTIVE);

            // then
            assertThat(store.getStatus()).isEqualTo(StoreStatus.INACTIVE);
        }

        @Test
        @DisplayName("CLOSED 상태에서 상태 변경 시도 시 BusinessException이 발생한다")
        void Store_changeStatus_invalidTransition_throwsException() {
            // given
            Store store = createStore(1L, 1L, "테스트 매장", StoreType.DIRECT);
            store.close();
            assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);

            // when & then
            assertThatThrownBy(() -> store.changeStatus(StoreStatus.ACTIVE))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Store 폐업 테스트")
    class CloseTest {

        @Test
        @DisplayName("close 호출 시 CLOSED 상태로 전환된다")
        void Store_close_success() {
            // given
            Store store = createStore(1L, 1L, "테스트 매장", StoreType.DIRECT);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);

            // when
            store.close();

            // then
            assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("Store 활성 상태 검증 테스트")
    class VerifyActiveTest {

        @Test
        @DisplayName("ACTIVE 상태인 경우 verifyActive가 예외를 발생시키지 않는다")
        void Store_isActive_returnsTrueWhenActive() {
            // given
            Store store = createStore(1L, 1L, "테스트 매장", StoreType.DIRECT);

            // when & then
            assertThatCode(store::verifyActive)
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("INACTIVE 상태인 경우 verifyActive가 BusinessException을 발생시킨다")
        void Store_verifyActive_throwsWhenInactive() {
            // given
            Store store = createStore(1L, 1L, "테스트 매장", StoreType.DIRECT);
            store.changeStatus(StoreStatus.INACTIVE);

            // when & then
            assertThatThrownBy(store::verifyActive)
                    .isInstanceOf(BusinessException.class);
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
}
