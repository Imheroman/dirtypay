package com.dirtypay.domain.store.repository;

import com.dirtypay.domain.store.entity.Store;
import com.dirtypay.domain.store.entity.StoreStatus;
import com.dirtypay.domain.store.entity.StoreType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 매장 리포지토리.
 *
 * <p>{@code @SQLRestriction("deleted_date IS NULL")}에 의해 삭제된 엔티티가
 * 모든 쿼리에서 자동으로 제외된다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
public interface StoreRepository extends JpaRepository<Store, Long> {

    /**
     * 매장 ID와 소유자 ID로 매장을 조회한다.
     *
     * <p>소유자 권한 검증 시 사용한다.</p>
     *
     * @param id      매장 ID
     * @param ownerId 소유자 회원 ID
     * @return 매장 Optional
     */
    Optional<Store> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * 사업자 등록번호로 매장을 조회한다.
     *
     * <p>중복 등록 여부 확인 시 사용한다.</p>
     *
     * @param businessNumber 사업자 등록번호
     * @return 매장 Optional
     */
    Optional<Store> findByBusinessNumber(String businessNumber);

    /**
     * 운영 상태로 매장 목록을 조회한다.
     *
     * <p>Soft Delete 필터가 자동 적용된다.</p>
     *
     * @param status 조회할 매장 상태
     * @return 해당 상태의 매장 목록
     */
    List<Store> findAllByStatus(StoreStatus status);

    /**
     * 특정 매장 유형을 제외한 매장 목록을 페이지 단위로 조회한다.
     *
     * <p>공개 목록 API에서 {@link StoreType#CUSTOM} 매장을 제외하는 데 사용된다.</p>
     *
     * @param storeType 제외할 매장 유형
     * @param status    매장 운영 상태
     * @param pageable  페이지 요청 정보
     * @return 조건에 맞는 매장 페이지
     */
    Page<Store> findAllByStoreTypeNotAndStatus(StoreType storeType, StoreStatus status, Pageable pageable);

    /**
     * 소유자 ID로 매장 목록을 페이지 단위로 조회한다.
     *
     * @param ownerId  매장 소유자 회원 ID
     * @param pageable 페이지 요청 정보
     * @return 해당 소유자의 매장 페이지
     */
    Page<Store> findAllByOwnerId(Long ownerId, Pageable pageable);
}
