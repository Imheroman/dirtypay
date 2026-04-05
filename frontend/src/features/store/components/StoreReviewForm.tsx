'use client';

import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useCreateStoreReviewMutation } from '../hooks/useCreateStoreReviewMutation';
import { useUpdateStoreReviewMutation } from '../hooks/useUpdateStoreReviewMutation';
import { useStoreReviewQuery } from '../hooks/useStoreReviewQuery';

// ---------------------------------------------------------------------------
// Schema
// ---------------------------------------------------------------------------

const reviewSchema = z.object({
  rating: z.number().min(1, '별점을 선택해 주세요').max(5),
  content: z.string().max(500, '500자 이내로 입력해 주세요').optional(),
});

type ReviewFormValues = z.infer<typeof reviewSchema>;

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

export interface StoreReviewFormProps {
  storeId: string | number;
  /** reviewId가 있으면 수정 모드입니다. */
  reviewId?: string | number;
  initialData?: {
    rating: number;
    content?: string;
  };
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

// ---------------------------------------------------------------------------
// StarPicker — 별점 선택 UI
// ---------------------------------------------------------------------------

interface StarPickerProps {
  value: number;
  hoveredRating: number;
  hasError: boolean;
  onChange: (rating: number) => void;
  onHover: (rating: number) => void;
  onLeave: () => void;
}

function StarPicker({
  value,
  hoveredRating,
  hasError,
  onChange,
  onHover,
  onLeave,
}: StarPickerProps) {
  const displayRating = hoveredRating > 0 ? hoveredRating : value;

  return (
    <div className="space-y-1.5">
      <Label>
        별점 <span className="text-destructive">*</span>
      </Label>
      <div
        className="flex items-center gap-1"
        role="radiogroup"
        aria-label="별점 선택"
        onMouseLeave={onLeave}
      >
        {Array.from({ length: 5 }, (_, i) => i + 1).map((star) => (
          <button
            key={star}
            type="button"
            role="radio"
            aria-checked={value === star}
            aria-label={`${star}점`}
            onClick={() => onChange(star)}
            onMouseEnter={() => onHover(star)}
            className={`text-2xl transition-transform hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 ${
              star <= displayRating
                ? 'text-yellow-400'
                : 'text-muted-foreground/30'
            }`}
          >
            ★
          </button>
        ))}
        {value > 0 && (
          <span className="ml-1.5 text-sm text-muted-foreground tabular-nums">
            {value}.0 / 5.0
          </span>
        )}
      </div>
      {hasError && (
        <p className="text-xs text-destructive" role="alert">
          별점을 선택해 주세요
        </p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// StoreReviewForm
// ---------------------------------------------------------------------------

export function StoreReviewForm({
  storeId,
  reviewId,
  initialData,
  isOpen,
  onOpenChange,
  onSuccess,
}: StoreReviewFormProps) {
  const [hoveredRating, setHoveredRating] = useState(0);
  const isEditMode = !!reviewId;

  const { data: fetchedReview } = useStoreReviewQuery(
    String(storeId),
    String(reviewId ?? '')
  );

  const {
    handleSubmit,
    setValue,
    watch,
    register,
    reset,
    formState: { errors },
  } = useForm<ReviewFormValues>({
    resolver: zodResolver(reviewSchema),
    defaultValues: { rating: 0, content: '' },
  });

  const rating = watch('rating');

  // 다이얼로그가 열릴 때 초기값 반영
  useEffect(() => {
    if (!isOpen) return;

    if (isEditMode && fetchedReview) {
      reset({
        rating: fetchedReview.rating,
        content: fetchedReview.content ?? '',
      });
    } else if (initialData) {
      reset({
        rating: initialData.rating,
        content: initialData.content ?? '',
      });
    } else {
      reset({ rating: 0, content: '' });
    }
  }, [isOpen, isEditMode, fetchedReview, initialData, reset]);

  const createMutation = useCreateStoreReviewMutation(() => {
    onSuccess?.();
    onOpenChange(false);
  });

  const updateMutation = useUpdateStoreReviewMutation(() => {
    onSuccess?.();
    onOpenChange(false);
  });

  const isMutating = createMutation.isPending || updateMutation.isPending;

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      reset({ rating: 0, content: '' });
      setHoveredRating(0);
    }
    onOpenChange(open);
  };

  const onSubmit = (values: ReviewFormValues) => {
    const request = {
      rating: values.rating,
      content: values.content?.trim() || undefined,
    };

    if (isEditMode) {
      updateMutation.mutate({
        storeId: String(storeId),
        reviewId: String(reviewId),
        request,
      });
    } else {
      createMutation.mutate({
        storeId: String(storeId),
        request,
      });
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditMode ? '리뷰 수정' : '리뷰 작성'}</DialogTitle>
          <DialogDescription>
            {isEditMode
              ? '리뷰 내용을 수정해 주세요.'
              : '이 매장에 대한 솔직한 리뷰를 남겨 주세요.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <StarPicker
            value={rating}
            hoveredRating={hoveredRating}
            hasError={!!errors.rating}
            onChange={(r) => setValue('rating', r, { shouldValidate: true })}
            onHover={setHoveredRating}
            onLeave={() => setHoveredRating(0)}
          />

          <div className="space-y-1.5">
            <Label htmlFor="review-content">
              내용{' '}
              <span className="text-xs font-normal text-muted-foreground">
                (선택)
              </span>
            </Label>
            <Textarea
              id="review-content"
              placeholder="매장 이용 경험을 자유롭게 적어 주세요."
              rows={4}
              maxLength={500}
              {...register('content')}
            />
            {errors.content && (
              <p className="text-xs text-destructive" role="alert">
                {errors.content.message}
              </p>
            )}
          </div>

          <DialogFooter className="gap-2 pt-1 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              className="bg-transparent"
              disabled={isMutating}
              onClick={() => handleOpenChange(false)}
            >
              취소
            </Button>
            <Button type="submit" disabled={isMutating || rating === 0}>
              {isMutating ? '저장 중...' : isEditMode ? '수정' : '저장'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
