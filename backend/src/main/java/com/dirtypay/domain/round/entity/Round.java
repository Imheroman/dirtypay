package com.dirtypay.domain.round.entity;

import com.dirtypay.global.common.enums.ErrorCode;
import com.dirtypay.global.common.entity.BaseEntity;
import com.dirtypay.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 라운드 엔티티.
 *
 * <p>세션 내에서 하나의 정산 단위(식사, 모임 등)를 나타낸다.
 * 각 라운드는 OPEN/CLOSED 상태를 가지며, OPEN 상태에서만 주문을 추가할 수 있다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "rounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class Round extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private String title;

    @Column
    private String place;

    @Column
    private LocalDate roundDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundStatus status;

    @Column(nullable = false)
    private int sortOrder;

    @Column(name = "store_id")
    private Long storeId;

    @Version
    private Long version;

    @Builder
    public Round(Long sessionId, String title, String place, LocalDate roundDate,
                 RoundStatus status, int sortOrder, Long storeId) {
        this.sessionId = sessionId;
        this.title = title;
        this.place = place;
        this.roundDate = roundDate;
        this.status = status != null ? status : RoundStatus.OPEN;
        this.sortOrder = sortOrder;
        this.storeId = storeId;
    }

    /**
     * 라운드 정보를 수정한다.
     *
     * @param title     제목
     * @param place     장소
     * @param roundDate 날짜
     * @param sortOrder 정렬 순서
     * @param storeId   가게 ID
     */
    public void update(String title, String place, LocalDate roundDate, int sortOrder,
                       Long storeId) {
        this.title = title;
        this.place = place;
        this.roundDate = roundDate;
        this.sortOrder = sortOrder;
        this.storeId = storeId;
    }

    /**
     * 라운드 상태를 변경한다.
     *
     * @param status 변경할 상태
     */
    public void changeStatus(RoundStatus status) {
        this.status = status;
    }

    /**
     * 라운드가 OPEN 상태인지 확인한다.
     *
     * @return OPEN 상태이면 true
     */
    public boolean isOpen() {
        return this.status == RoundStatus.OPEN;
    }

    /**
     * 라운드가 OPEN 상태인지 검증한다.
     *
     * @throws BusinessException OPEN 상태가 아닌 경우 ROUND_ALREADY_CLOSED 에러
     */
    public void verifyOpen() {
        if (!this.isOpen()) {
            throw new BusinessException(ErrorCode.ROUND_ALREADY_CLOSED);
        }
    }

    /**
     * 라운드가 CLOSED 상태인지 확인한다.
     *
     * @return CLOSED 상태이면 true
     */
    public boolean isClosed() {
        return this.status == RoundStatus.CLOSED;
    }
}
