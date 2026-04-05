'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

/**
 * 세션 카드 Skeleton
 */
export function SessionCardSkeleton() {
  return (
    <Card className="overflow-hidden">
      <CardContent className="p-4">
        <div className="flex items-start justify-between mb-3">
          <div className="flex-1">
            <Skeleton className="h-6 w-32 mb-2" />
            <Skeleton className="h-4 w-48" />
          </div>
          <Skeleton className="h-6 w-16 rounded-full" />
        </div>

        <div className="flex items-center gap-4 pt-3 border-t border-border">
          <div className="flex items-center gap-1">
            <Skeleton className="h-4 w-4" />
            <Skeleton className="h-4 w-12" />
          </div>
          <div className="flex items-center gap-1">
            <Skeleton className="h-4 w-4" />
            <Skeleton className="h-4 w-8" />
          </div>
          <div className="flex items-center gap-1 ml-auto">
            <Skeleton className="h-4 w-20" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * 세션 목록 Skeleton
 */
export function SessionListSkeleton() {
  return (
    <div className="space-y-4">
      {/* 헤더 영역 */}
      <div className="flex items-center justify-between mb-6">
        <Skeleton className="h-8 w-24" />
        <Skeleton className="h-10 w-28 rounded-lg" />
      </div>

      {/* 세션 카드 목록 */}
      <div className="space-y-3">
        {[1, 2, 3, 4].map((i) => (
          <SessionCardSkeleton key={i} />
        ))}
      </div>
    </div>
  );
}

/**
 * 세션 대시보드 Skeleton
 */
export function SessionDashboardSkeleton() {
  return (
    <div className="space-y-6">
      {/* 세션 정보 카드 */}
      <Card>
        <CardContent className="p-5">
          <div className="flex items-center justify-between mb-4">
            <Skeleton className="h-7 w-40" />
            <Skeleton className="h-6 w-16 rounded-full" />
          </div>
          <Skeleton className="h-4 w-full mb-2" />
          <Skeleton className="h-4 w-3/4" />
        </CardContent>
      </Card>

      {/* 통계 카드들 */}
      <div className="grid grid-cols-2 gap-3">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i}>
            <CardContent className="p-4 text-center">
              <Skeleton className="h-4 w-16 mx-auto mb-2" />
              <Skeleton className="h-8 w-20 mx-auto" />
            </CardContent>
          </Card>
        ))}
      </div>

      {/* 최근 라운드 */}
      <div>
        <Skeleton className="h-6 w-24 mb-3" />
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <Skeleton className="h-5 w-28 mb-1" />
                    <Skeleton className="h-4 w-20" />
                  </div>
                  <Skeleton className="h-5 w-20" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
