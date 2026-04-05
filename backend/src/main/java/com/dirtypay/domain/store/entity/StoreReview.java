package com.dirtypay.domain.store.entity;

import com.dirtypay.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 매장 리뷰 엔티티.
 *
 * <p>회원({@code memberId})이 매장({@code storeId})에 대해 작성한 리뷰를 나타낸다.
 * {@code rating}은 1~5 범위의 정수이며, DTO 계층에서 Bean Validation으로 검증한다.</p>
 *
 * <p>Soft Delete: {@code deleted_date IS NULL} 조건으로 삭제된 레코드를
 * 자동 제외한다.</p>
 *
 * @author kim-young-woong
 * @since 1.0.0
 */
@Entity
@Table(name = "store_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class StoreReview extends BaseEntity {

    /**
     * 리뷰 대상 매장 ID. FK → stores.
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     * 리뷰 작성자 회원 ID. FK → members (검증은 Service에서 수행).
     */
    @Column(nullable = false)
    private Long memberId;

    /**
     * 별점. 1~5 범위의 정수.
     */
    @Column(nullable = false)
    private int rating;

    /**
     * 리뷰 내용. 최대 1000자. 선택 항목.
     */
    @Column(length = 1000)
    private String content;

    /**
     * StoreReview 엔티티 생성자.
     *
     * @param storeId  리뷰 대상 매장 ID
     * @param memberId 작성자 회원 ID
     * @param rating   별점 (1~5)
     * @param content  리뷰 내용
     */
    @Builder
    public StoreReview(Long storeId, Long memberId, int rating, String content) {
        this.storeId = storeId;
        this.memberId = memberId;
        this.rating = rating;
        this.content = content;
    }

    /**
     * 리뷰 내용을 수정한다.
     *
     * @param rating  변경할 별점 (1~5)
     * @param content 변경할 리뷰 내용
     */
    public void update(int rating, String content) {
        this.rating = rating;
        this.content = content;
    }
}
