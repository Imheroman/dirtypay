'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { formatDate } from '@/lib/format';
import { useDeleteStoreReviewMutation } from '../hooks/useDeleteStoreReviewMutation';
import type { StoreReview } from '../types';

export interface StoreReviewCardProps {
  review: StoreReview;
  storeId: string | number;
  /** 본인 리뷰일 때만 수정/삭제 버튼을 렌더링합니다. */
  isCurrentUserReview?: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
}

interface StarDisplayProps {
  rating: number;
}

function StarDisplay({ rating }: StarDisplayProps) {
  const rounded = Math.round(rating);
  return (
    <div
      className="flex items-center gap-0.5"
      aria-label={`별점 ${rating}점 / 5점`}
    >
      {Array.from({ length: 5 }).map((_, i) => (
        <span
          key={i}
          className={
            i < rounded ? 'text-yellow-400' : 'text-muted-foreground/30'
          }
          aria-hidden="true"
        >
          ★
        </span>
      ))}
      <span className="ml-1.5 text-sm font-medium tabular-nums">
        {rating.toFixed(1)}
        <span className="ml-0.5 font-normal text-muted-foreground">/ 5.0</span>
      </span>
    </div>
  );
}

export function StoreReviewCard({
  review,
  storeId,
  isCurrentUserReview = false,
  onEdit,
  onDelete,
}: StoreReviewCardProps) {
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const deleteMutation = useDeleteStoreReviewMutation(() => {
    setIsDeleteOpen(false);
    onDelete?.();
  });

  const handleDeleteConfirm = () => {
    deleteMutation.mutate({
      storeId: String(storeId),
      reviewId: String(review.id),
    });
  };

  return (
    <>
      <article className="space-y-2 rounded-lg border bg-card p-4 transition-shadow hover:shadow-sm">
        <div className="flex items-start justify-between gap-2">
          <StarDisplay rating={review.rating} />
          <time
            dateTime={review.createdDate}
            className="shrink-0 text-xs text-muted-foreground"
          >
            {formatDate(review.createdDate)}
          </time>
        </div>

        {review.content && (
          <p className="text-sm leading-relaxed text-foreground">
            {review.content}
          </p>
        )}

        {isCurrentUserReview && (
          <div className="flex items-center gap-2 border-t pt-2">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-7 px-2 text-xs"
              onClick={onEdit}
              aria-label="리뷰 수정"
            >
              수정
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-7 px-2 text-xs text-destructive hover:text-destructive"
              onClick={() => setIsDeleteOpen(true)}
              aria-label="리뷰 삭제"
            >
              삭제
            </Button>
          </div>
        )}
      </article>

      <ConfirmModal
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        title="리뷰를 삭제할까요?"
        description="삭제하면 되돌릴 수 없어요."
        confirmText="삭제"
        variant="destructive"
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}
