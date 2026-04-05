package com.dirtypay.domain.store.strategy;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DirectRegistrationStrategy}, {@link PosIntegrationStrategy},
 * {@link CustomRegistrationStrategy} 단위 테스트.
 *
 * <p>각 전략의 supports() 반환 타입, validate() 검증 로직을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
class StoreRegistrationStrategyTest {

    private final DirectRegistrationStrategy directStrategy = new DirectRegistrationStrategy();
    private final PosIntegrationStrategy posStrategy = new PosIntegrationStrategy();
    private final CustomRegistrationStrategy customStrategy = new CustomRegistrationStrategy();

    @Nested
    @DisplayName("DirectRegistrationStrategy 테스트")
    class DirectRegistrationStrategyTests {

        @Test
        @DisplayName("supports()는 StoreType.DIRECT를 반환한다")
        void DirectRegistrationStrategy_register_createsStoreWithDirectType() {
            // when
            StoreType supportedType = directStrategy.supports();

            // then
            assertThat(supportedType).isEqualTo(StoreType.DIRECT);
        }

        @Test
        @DisplayName("validate() 호출 시 posIntegrationKey 값에 무관하게 예외가 발생하지 않는다")
        void DirectRegistrationStrategy_validate_alwaysSuccess() {
            // when & then — null key도 예외 없음
            assertThatCode(() -> directStrategy.validate(StoreType.DIRECT, null))
                    .doesNotThrowAnyException();

            assertThatCode(() -> directStrategy.validate(StoreType.DIRECT, "someKey"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("onRegister() 호출 시 예외가 발생하지 않는다")
        void DirectRegistrationStrategy_onRegister_noException() {
            // given
            Store store = createStore(1L, StoreType.DIRECT);

            // when & then
            assertThatCode(() -> directStrategy.onRegister(store))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("PosIntegrationStrategy 테스트")
    class PosIntegrationStrategyTests {

        @Test
        @DisplayName("supports()는 StoreType.POS_INTEGRATED를 반환한다")
        void PosRegistrationStrategy_register_createsStoreWithPosType() {
            // when
            StoreType supportedType = posStrategy.supports();

            // then
            assertThat(supportedType).isEqualTo(StoreType.POS_INTEGRATED);
        }

        @Test
        @DisplayName("posIntegrationKey가 null이면 BusinessException이 발생한다")
        void PosRegistrationStrategy_register_nullKey_throwsException() {
            // when & then
            assertThatThrownBy(() -> posStrategy.validate(StoreType.POS_INTEGRATED, null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("posIntegrationKey가 공백이면 BusinessException이 발생한다")
        void PosRegistrationStrategy_validate_blankKey_throwsException() {
            // when & then
            assertThatThrownBy(() -> posStrategy.validate(StoreType.POS_INTEGRATED, "   "))
                    .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> posStrategy.validate(StoreType.POS_INTEGRATED, ""))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("유효한 posIntegrationKey가 있으면 validate()가 예외를 발생시키지 않는다")
        void PosRegistrationStrategy_validate_validKey_success() {
            // when & then
            assertThatCode(() -> posStrategy.validate(StoreType.POS_INTEGRATED, "valid-pos-key-12345"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("onRegister() 호출 시 예외가 발생하지 않는다")
        void PosRegistrationStrategy_onRegister_noException() {
            // given
            Store store = createStore(1L, StoreType.POS_INTEGRATED);
            ReflectionTestUtils.setField(store, "posIntegrationKey", "valid-pos-key-12345");

            // when & then
            assertThatCode(() -> posStrategy.onRegister(store))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("CustomRegistrationStrategy 테스트")
    class CustomRegistrationStrategyTests {

        @Test
        @DisplayName("supports()는 StoreType.CUSTOM을 반환한다")
        void CustomRegistrationStrategy_supports_returnsCustomType() {
            // when
            StoreType supportedType = customStrategy.supports();

            // then
            assertThat(supportedType).isEqualTo(StoreType.CUSTOM);
        }

        @Test
        @DisplayName("validate() 호출 시 posIntegrationKey 값에 무관하게 예외가 발생하지 않는다")
        void CustomRegistrationStrategy_validate_alwaysSuccess() {
            // when & then — null key도 예외 없음
            assertThatCode(() -> customStrategy.validate(StoreType.CUSTOM, null))
                    .doesNotThrowAnyException();

            assertThatCode(() -> customStrategy.validate(StoreType.CUSTOM, "someKey"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("onRegister() 호출 시 예외가 발생하지 않는다")
        void CustomRegistrationStrategy_onRegister_noException() {
            // given
            Store store = createStore(1L, StoreType.CUSTOM);

            // when & then
            assertThatCode(() -> customStrategy.onRegister(store))
                    .doesNotThrowAnyException();
        }
    }

    // === Helper Methods ===

    private Store createStore(Long id, StoreType storeType) {
        Store store = Store.builder()
                .ownerId(1L)
                .name("테스트 매장")
                .businessNumber("123-45-67890")
                .address("서울시 강남구")
                .storeType(storeType)
                .build();
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }
}
