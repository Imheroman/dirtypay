package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import com.dirtypay.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link StoreRepository} 통합 테스트.
 *
 * <p>Store 저장/조회/소프트 삭제 필터링, 사업자번호 UNIQUE 제약을 검증한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@DataJpaTest
@Import(JpaConfig.class)
class StoreRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    private Store activeStore;
    private Store inactiveStore;

    @BeforeEach
    void setUp() {
        storeRepository.deleteAll();

        // ACTIVE 매장
        activeStore = storeRepository.save(Store.builder()
                .ownerId(1L)
                .name("활성 매장")
                .businessNumber("111-11-11111")
                .address("서울시 강남구")
                .storeType(StoreType.DIRECT)
                .status(StoreStatus.ACTIVE)
                .build());

        // INACTIVE 매장
        inactiveStore = storeRepository.save(Store.builder()
                .ownerId(2L)
                .name("비활성 매장")
                .businessNumber("222-22-22222")
                .address("서울시 서초구")
                .storeType(StoreType.DIRECT)
                .status(StoreStatus.ACTIVE)
                .build());
        inactiveStore.changeStatus(StoreStatus.INACTIVE);
        storeRepository.save(inactiveStore);

        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Store 저장 테스트")
    class SaveTest {

        @Test
        @DisplayName("Store 저장 후 ID가 자동 생성되고 조회 가능하다")
        void StoreRepository_save_success() {
            // given
            Store newStore = Store.builder()
                    .ownerId(3L)
                    .name("새 매장")
                    .businessNumber("333-33-33333")
                    .address("부산시 해운대구")
                    .storeType(StoreType.POS_INTEGRATED)
                    .status(StoreStatus.ACTIVE)
                    .posIntegrationKey("pos-key-abc")
                    .build();

            // when
            Store saved = storeRepository.save(newStore);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("새 매장");
            assertThat(saved.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("사업자번호 조회 테스트")
    class FindByBusinessNumberTest {

        @Test
        @DisplayName("사업자번호로 매장 조회 시 일치하는 매장을 반환한다")
        void StoreRepository_findByBusinessNumber_returnsStore() {
            // when
            Optional<Store> result = storeRepository.findByBusinessNumber("111-11-11111");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("활성 매장");
        }

        @Test
        @DisplayName("존재하지 않는 사업자번호 조회 시 빈 Optional을 반환한다")
        void StoreRepository_findByBusinessNumber_returnsEmptyWhenNotFound() {
            // when
            Optional<Store> result = storeRepository.findByBusinessNumber("999-99-99999");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("상태별 조회 테스트")
    class FindByStatusTest {

        @Test
        @DisplayName("findAllByStatus(ACTIVE)는 활성 매장만 반환한다")
        void StoreRepository_findByStatus_returnsActiveOnly() {
            // when
            List<Store> activeStores = storeRepository.findAllByStatus(StoreStatus.ACTIVE);

            // then
            assertThat(activeStores).hasSize(1);
            assertThat(activeStores.get(0).getName()).isEqualTo("활성 매장");
        }

        @Test
        @DisplayName("findAllByStatus(INACTIVE)는 비활성 매장만 반환한다")
        void StoreRepository_findByStatus_returnsInactiveOnly() {
            // when
            List<Store> inactiveStores = storeRepository.findAllByStatus(StoreStatus.INACTIVE);

            // then
            assertThat(inactiveStores).hasSize(1);
            assertThat(inactiveStores.get(0).getName()).isEqualTo("비활성 매장");
        }
    }

    @Nested
    @DisplayName("소프트 삭제 필터링 테스트")
    class SoftDeleteTest {

        @Test
        @DisplayName("@SQLRestriction: Soft Delete된 매장은 조회에서 자동으로 제외된다")
        void StoreRepository_sqlRestriction_excludesDeleted() {
            // given — 전체 목록에서 삭제 전 확인
            List<Store> beforeDelete = storeRepository.findAll();
            int countBefore = beforeDelete.size();

            Store storeToDelete = storeRepository.findById(activeStore.getId()).get();
            storeToDelete.delete();
            storeRepository.save(storeToDelete);
            entityManager.flush();
            entityManager.clear();

            // when
            List<Store> afterDelete = storeRepository.findAll();

            // then
            assertThat(afterDelete).hasSize(countBefore - 1);
            assertThat(afterDelete).extracting(Store::getName).doesNotContain("활성 매장");
        }
    }

    @Nested
    @DisplayName("사업자번호 UNIQUE 제약 테스트")
    class UniqueConstraintTest {

        @Test
        @DisplayName("동일한 사업자번호로 저장 시 DataIntegrityViolationException이 발생한다")
        void StoreRepository_businessNumber_uniqueConstraint() {
            // given — "111-11-11111"은 setUp에서 이미 저장됨
            Store duplicateStore = Store.builder()
                    .ownerId(10L)
                    .name("중복 사업자번호 매장")
                    .businessNumber("111-11-11111")  // 중복
                    .address("서울시 용산구")
                    .storeType(StoreType.DIRECT)
                    .status(StoreStatus.ACTIVE)
                    .build();

            // when & then
            assertThatThrownBy(() -> {
                storeRepository.save(duplicateStore);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
