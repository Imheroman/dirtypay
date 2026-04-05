'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

/**
 * 라운드 상세 헤더 Skeleton
 */
export function RoundHeaderSkeleton() {
  return (
    <Card className="mb-6 bg-primary/5 border-primary/20">
      <CardContent className="p-5">
        <div className="flex items-center justify-between">
          <div>
            <Skeleton className="h-4 w-24 mb-2" />
            <Skeleton className="h-9 w-32" />
          </div>
          <div className="text-right">
            <Skeleton className="h-4 w-12 mb-2" />
            <Skeleton className="h-8 w-10" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * 참여자 탭 Skeleton
 */
export function ParticipantsTabSkeleton() {
  return (
    <div className="space-y-3">
      {/* 참여자 수 헤더 */}
      <div className="flex items-center justify-between mb-4">
        <Skeleton className="h-5 w-24" />
        <Skeleton className="h-9 w-20 rounded-lg" />
      </div>

      {/* 참여자 목록 */}
      {[1, 2, 3, 4, 5].map((i) => (
        <Card key={i}>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <Skeleton className="w-10 h-10 rounded-full" />
              <div className="flex-1">
                <Skeleton className="h-5 w-20 mb-1" />
                <Skeleton className="h-4 w-16" />
              </div>
              <Skeleton className="h-5 w-20" />
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

/**
 * 주문 현황 탭 Skeleton
 */
export function OrdersTabSkeleton() {
  return (
    <div className="space-y-4">
      {/* 카테고리별 주문 */}
      {[1, 2, 3].map((category) => (
        <Card key={category}>
          <CardContent className="p-4">
            {/* 카테고리 헤더 */}
            <div className="flex items-center justify-between mb-4">
              <Skeleton className="h-5 w-16" />
              <Skeleton className="h-5 w-20" />
            </div>

            {/* 메뉴 아이템들 */}
            <div className="space-y-3">
              {[1, 2].map((item) => (
                <div key={item} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Skeleton className="w-12 h-12 rounded-lg" />
                    <div>
                      <Skeleton className="h-5 w-24 mb-1" />
                      <Skeleton className="h-4 w-16" />
                    </div>
                  </div>
                  <Skeleton className="h-5 w-16" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

/**
 * 그룹 탭 Skeleton
 */
export function GroupsTabSkeleton() {
  return (
    <div className="space-y-4">
      {/* 그룹 생성 버튼 */}
      <div className="flex justify-end mb-4">
        <Skeleton className="h-9 w-28 rounded-lg" />
      </div>

      {/* 그룹 버블들 */}
      <div className="space-y-4">
        {[1, 2, 3].map((group) => (
          <Card key={group} className="overflow-hidden">
            <CardContent className="p-4">
              {/* 그룹 헤더 */}
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <Skeleton className="h-6 w-24" />
                  <Skeleton className="h-5 w-12 rounded-full" />
                </div>
                <Skeleton className="h-8 w-8 rounded-full" />
              </div>

              {/* 멤버 아바타들 */}
              <div className="flex -space-x-2 mb-3">
                {[1, 2, 3, 4].map((avatar) => (
                  <Skeleton
                    key={avatar}
                    className="w-8 h-8 rounded-full border-2 border-background"
                  />
                ))}
              </div>

              {/* 공유 메뉴 */}
              <div className="pt-3 border-t border-border">
                <Skeleton className="h-4 w-20 mb-2" />
                <div className="flex gap-2">
                  <Skeleton className="h-6 w-16 rounded-full" />
                  <Skeleton className="h-6 w-20 rounded-full" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

/**
 * 메뉴판 탭 Skeleton
 */
export function MenuTabSkeleton() {
  return (
    <div className="space-y-4">
      {/* 검색 & 필터 */}
      <div className="flex gap-2 mb-4">
        <Skeleton className="h-10 flex-1 rounded-lg" />
        <Skeleton className="h-10 w-20 rounded-lg" />
      </div>

      {/* 카테고리 탭 */}
      <div className="flex gap-2 mb-4 overflow-x-auto">
        {[1, 2, 3, 4].map((tab) => (
          <Skeleton key={tab} className="h-9 w-16 rounded-full shrink-0" />
        ))}
      </div>

      {/* 메뉴 그리드 */}
      <div className="grid grid-cols-2 gap-3">
        {[1, 2, 3, 4, 5, 6].map((menu) => (
          <Card key={menu}>
            <CardContent className="p-3">
              <Skeleton className="w-full aspect-square rounded-lg mb-2" />
              <Skeleton className="h-5 w-full mb-1" />
              <Skeleton className="h-4 w-16" />
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

/**
 * 내 주문 탭 Skeleton
 */
export function MyOrderTabSkeleton() {
  return (
    <div className="space-y-4">
      {/* 주문 요약 카드 */}
      <Card className="bg-primary/5 border-primary/20">
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-7 w-24" />
          </div>
        </CardContent>
      </Card>

      {/* 주문 리스트 */}
      <div className="space-y-3">
        {[1, 2, 3].map((order) => (
          <Card key={order}>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Skeleton className="w-12 h-12 rounded-lg" />
                  <div>
                    <Skeleton className="h-5 w-24 mb-1" />
                    <Skeleton className="h-4 w-16" />
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Skeleton className="h-8 w-8 rounded-lg" />
                  <Skeleton className="h-5 w-6" />
                  <Skeleton className="h-8 w-8 rounded-lg" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* 주문 추가 버튼 */}
      <Skeleton className="h-12 w-full rounded-lg" />
    </div>
  );
}

/**
 * 라운드 상세 페이지 전체 Skeleton
 */
export function RoundDetailPageSkeleton() {
  return (
    <div>
      {/* 헤더 */}
      <RoundHeaderSkeleton />

      {/* 탭 */}
      <div className="flex gap-1 mb-6">
        {[1, 2, 3, 4, 5].map((tab) => (
          <Skeleton key={tab} className="h-10 flex-1 rounded-lg" />
        ))}
      </div>

      {/* 콘텐츠 영역 */}
      <OrdersTabSkeleton />
    </div>
  );
}
