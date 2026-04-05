'use client';

import { useState } from 'react';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ChevronLeftIcon, ChevronRightIcon } from '@/components/common/Icons';
import { useStoreOrdersQuery } from '../hooks/useStoreOrdersQuery';
import { StoreOrderCard } from './StoreOrderCard';
import type { StoreOrderStatus } from '../types';

export interface StoreOrderListProps {
  storeId: string | number;
  onSelectOrder?: (orderId: number) => void;
}

type FilterTab = 'ALL' | StoreOrderStatus;

const FILTER_TABS: Array<{ value: FilterTab; label: string }> = [
  { value: 'ALL', label: '전체' },
  { value: 'PENDING', label: '대기' },
  { value: 'CONFIRMED', label: '확인' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'CANCELLED', label: '취소' },
];

function OrderCardSkeleton() {
  return (
    <div className="space-y-3 rounded-lg border p-4">
      <div className="flex items-start justify-between">
        <Skeleton className="h-4 w-28" />
        <Skeleton className="h-5 w-14 rounded-full" />
      </div>
      <Skeleton className="h-4 w-40" />
      <Skeleton className="h-3 w-32" />
    </div>
  );
}

function OrderListSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 3 }).map((_, i) => (
        <OrderCardSkeleton key={i} />
      ))}
    </div>
  );
}

export function StoreOrderList({ storeId, onSelectOrder }: StoreOrderListProps) {
  const [activeTab, setActiveTab] = useState<FilterTab>('ALL');
  const [currentPage, setCurrentPage] = useState(0);

  const statusParam = activeTab === 'ALL' ? undefined : activeTab;
  const { data, isLoading, isError } = useStoreOrdersQuery(String(storeId), {
    page: currentPage,
    size: 10,
    ...(statusParam ? { status: statusParam } : {}),
  });

  const handleTabChange = (value: string) => {
    setActiveTab(value as FilterTab);
    setCurrentPage(0);
  };

  const totalPages = data?.page?.totalPages ?? 0;
  const orders = data?.content ?? [];

  return (
    <Tabs value={activeTab} onValueChange={handleTabChange}>
      <TabsList className="mb-4 flex h-auto flex-wrap gap-1 bg-transparent p-0">
        {FILTER_TABS.map((tab) => (
          <TabsTrigger
            key={tab.value}
            value={tab.value}
            className="rounded-full border border-border px-3 py-1 text-sm data-[state=active]:border-primary data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
          >
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>

      {FILTER_TABS.map((tab) => (
        <TabsContent key={tab.value} value={tab.value} className="mt-0">
          {isLoading ? (
            <OrderListSkeleton />
          ) : isError ? (
            <div className="py-8 text-center text-sm text-muted-foreground">
              주문 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
            </div>
          ) : orders.length === 0 ? (
            <div className="py-12 text-center text-sm text-muted-foreground">
              아직 주문이 없어요
            </div>
          ) : (
            <div className="space-y-3" role="list" aria-label={`${tab.label} 주문 목록`}>
              {orders.map((order) => (
                <div key={order.id} role="listitem">
                  <StoreOrderCard
                    order={order}
                    storeId={storeId}
                    onStatusChange={() => onSelectOrder?.(order.id)}
                    onCancel={() => onSelectOrder?.(order.id)}
                  />
                </div>
              ))}
            </div>
          )}

          {!isLoading && !isError && totalPages > 1 && (
            <nav
              className="mt-4 flex items-center justify-center gap-2"
              aria-label="주문 목록 페이지네이션"
            >
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                disabled={currentPage === 0}
                aria-label="이전 페이지"
                className="h-8 w-8 p-0"
              >
                <ChevronLeftIcon className="h-4 w-4" aria-hidden="true" />
              </Button>

              {Array.from({ length: totalPages }).map((_, pageIndex) => (
                <Button
                  key={pageIndex}
                  variant={currentPage === pageIndex ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => setCurrentPage(pageIndex)}
                  aria-label={`${pageIndex + 1}페이지`}
                  aria-current={currentPage === pageIndex ? 'page' : undefined}
                  className="h-8 w-8 p-0 text-xs"
                >
                  {pageIndex + 1}
                </Button>
              ))}

              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((prev) => Math.min(totalPages - 1, prev + 1))}
                disabled={currentPage >= totalPages - 1}
                aria-label="다음 페이지"
                className="h-8 w-8 p-0"
              >
                <ChevronRightIcon className="h-4 w-4" aria-hidden="true" />
              </Button>
            </nav>
          )}
        </TabsContent>
      ))}
    </Tabs>
  );
}
