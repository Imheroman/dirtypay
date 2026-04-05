'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { useStoreReviewsQuery } from '../hooks/useStoreReviewsQuery';
import { useStoreAverageRatingQuery } from '../hooks/useStoreAverageRatingQuery';
import { StoreReviewCard } from './StoreReviewCard';
import { StoreReviewForm } from './StoreReviewForm';
import type { StoreReview } from '../types';

export interface StoreReviewListProps {
  storeId: string | number;
  /** 현재 로그인한 회원 ID. 본인 리뷰 여부 판단에 사용됩니다. */
  currentMemberId?: number | null;
  /** 리뷰 작성 버튼 표시 여부 */
  canWrite?: boolean;
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

function StarDisplay({ rating }: { rating: number }) {
  const rounded = Math.round(rating);
  return (
    <div className="flex items-center gap-0.5" aria-label={`평균 별점 ${rating}점`}>
      {Array.from({ length: 5 }).map((_, i) => (
        <span
          key={i}
          className={i < rounded ? 'text-yellow-400' : 'text-muted-foreground/30'}
          aria-hidden="true"
        >
          ★
        </span>
      ))}
    </div>
  );
}

function ReviewSkeleton() {
  return (
    <div className="space-y-3" aria-busy="true" aria-label="리뷰 불러오는 중">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="space-y-2 rounded-lg border p-4">
          <div className="flex items-center justify-between">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-3 w-20" />
          </div>
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-3/4" />
        </div>
      ))}
    </div>
  );
}

// ---------------------------------------------------------------------------
// StoreReviewList
// ---------------------------------------------------------------------------

export function StoreReviewList({
  storeId,
  currentMemberId,
  canWrite = false,
}: StoreReviewListProps) {
  const [editTarget, setEditTarget] = useState<StoreReview | null>(null);
  const [isWriteOpen, setIsWriteOpen] = useState(false);

  const { data: reviews = [], isLoading, isError } = useStoreReviewsQuery(String(storeId));
  const { data: averageRating } = useStoreAverageRatingQuery(String(storeId));

  if (isLoading) {
    return <ReviewSkeleton />;
  }

  if (isError) {
    return (
      <div className="py-8 text-center text-sm text-muted-foreground">
        리뷰를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 상단 요약 + 리뷰 작성 버튼 */}
      <div className="flex items-center justify-between">
        {averageRating !== undefined && reviews.length > 0 ? (
          <div className="flex items-center gap-2">
            <StarDisplay rating={averageRating} />
            <span className="text-sm font-medium tabular-nums">
              {averageRating.toFixed(1)}
              <span className="ml-1 font-normal text-muted-foreground">/ 5</span>
            </span>
            <span className="text-xs text-muted-foreground">
              ({reviews.length}개 리뷰)
            </span>
          </div>
        ) : (
          <div />
        )}
        {canWrite && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setIsWriteOpen(true)}
          >
            리뷰 작성
          </Button>
        )}
      </div>

      {/* 리뷰 목록 */}
      {reviews.length === 0 ? (
        <div className="py-12 text-center text-sm text-muted-foreground">
          아직 등록된 리뷰가 없어요
        </div>
      ) : (
        <div className="space-y-3" role="list" aria-label="리뷰 목록">
          {reviews.map((review) => (
            <div key={review.id} role="listitem">
              <StoreReviewCard
                review={review}
                storeId={storeId}
                isCurrentUserReview={
                  currentMemberId != null && review.memberId === currentMemberId
                }
                onEdit={() => setEditTarget(review)}
              />
            </div>
          ))}
        </div>
      )}

      {/* 리뷰 작성 다이얼로그 */}
      <StoreReviewForm
        storeId={storeId}
        isOpen={isWriteOpen}
        onOpenChange={setIsWriteOpen}
      />

      {/* 리뷰 수정 다이얼로그 */}
      {editTarget && (
        <StoreReviewForm
          storeId={storeId}
          reviewId={editTarget.id}
          initialData={{
            rating: editTarget.rating,
            content: editTarget.content ?? undefined,
          }}
          isOpen={!!editTarget}
          onOpenChange={(open) => {
            if (!open) setEditTarget(null);
          }}
        />
      )}
    </div>
  );
}
