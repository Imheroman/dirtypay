'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

/**
 * 정산 요약 카드 Skeleton
 */
export function SettlementSummarySkeleton() {
  return (
    <Card className="mb-6">
      <CardContent className="p-5">
        {/* 총 정산 금액 */}
        <div className="text-center mb-4">
          <Skeleton className="h-4 w-20 mx-auto mb-2" />
          <Skeleton className="h-9 w-36 mx-auto" />
        </div>

        {/* 진행률 */}
        <div className="space-y-2 mb-4">
          <div className="flex items-center justify-between">
            <Skeleton className="h-4 w-16" />
            <Skeleton className="h-4 w-10" />
          </div>
          <Skeleton className="h-2 w-full" />
        </div>

        {/* 정산 완료 / 대기 */}
        <div className="grid grid-cols-2 gap-4 pt-4 border-t border-border">
          <div className="text-center">
            <Skeleton className="h-4 w-16 mx-auto mb-2" />
            <Skeleton className="h-6 w-24 mx-auto" />
          </div>
          <div className="text-center">
            <Skeleton className="h-4 w-16 mx-auto mb-2" />
            <Skeleton className="h-6 w-24 mx-auto" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * 멤버 정산 카드 Skeleton
 */
export function MemberSettlementSkeleton() {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center gap-3 mb-3">
          {/* 아바타 */}
          <Skeleton className="w-10 h-10 rounded-full" />

          {/* 이름 & 그룹 */}
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-1">
              <Skeleton className="h-5 w-16" />
              <Skeleton className="h-5 w-12 rounded-full" />
            </div>
            <Skeleton className="h-4 w-24" />
          </div>

          {/* 금액 & 상태 */}
          <div className="text-right">
            <Skeleton className="h-5 w-20 mb-1" />
            <Skeleton className="h-5 w-16 rounded-full" />
          </div>
        </div>

        {/* 라운드별 상세 */}
        <div className="space-y-2 pt-3 border-t border-border">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center justify-between">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-16" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * 라운드 정산 카드 Skeleton
 */
export function RoundSettlementSkeleton() {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-2">
          <Skeleton className="h-5 w-24" />
          <Skeleton className="h-5 w-20" />
        </div>
        <div className="flex items-center justify-between mb-2">
          <Skeleton className="h-4 w-28" />
          <Skeleton className="h-4 w-16" />
        </div>
        <div className="pt-2 border-t border-border">
          <Skeleton className="h-4 w-32" />
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * 정산 페이지 전체 Skeleton
 */
export function SettlementPageSkeleton() {
  return (
    <div className="space-y-4">
      {/* 요약 카드 */}
      <SettlementSummarySkeleton />

      {/* 탭 Skeleton */}
      <div className="flex gap-2 mb-4">
        <Skeleton className="h-10 flex-1 rounded-lg" />
        <Skeleton className="h-10 flex-1 rounded-lg" />
        <Skeleton className="h-10 flex-1 rounded-lg" />
      </div>

      {/* 멤버 리스트 */}
      <div className="space-y-3">
        {[1, 2, 3, 4].map((i) => (
          <MemberSettlementSkeleton key={i} />
        ))}
      </div>
    </div>
  );
}
