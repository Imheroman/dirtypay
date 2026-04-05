'use client';

import { useEffect, useRef, useCallback } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { formatAmount, formatDateTime } from '@/lib/format';
import { cn } from '@/lib/utils';
import { useWalletTransactionsQuery } from '../hooks';
import type { TransactionType } from '../types';

const typeConfig: Record<TransactionType, { label: string; color: string; sign: string }> = {
  CHARGE: { label: '충전', color: 'text-blue-600', sign: '+' },
  TRANSFER_IN: { label: '받은 돈', color: 'text-green-600', sign: '+' },
  TRANSFER_OUT: { label: '보낸 돈', color: 'text-red-600', sign: '-' },
  REFUND: { label: '환불', color: 'text-muted-foreground', sign: '+' },
};

export function TransactionList() {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } =
    useWalletTransactionsQuery();
  const observerRef = useRef<HTMLDivElement>(null);

  const handleObserver = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const [entry] = entries;
      if (entry.isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    },
    [fetchNextPage, hasNextPage, isFetchingNextPage]
  );

  useEffect(() => {
    const el = observerRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(handleObserver, { threshold: 0.1 });
    observer.observe(el);
    return () => observer.disconnect();
  }, [handleObserver]);

  const transactions = data?.pages.flatMap((page) => page.content) ?? [];

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Card key={i}>
            <CardContent className="p-4">
              <div className="animate-pulse space-y-2">
                <div className="h-4 bg-muted rounded w-20" />
                <div className="h-5 bg-muted rounded w-32" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  if (transactions.length === 0) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-muted-foreground">아직 거래 내역이 없어요</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-2">
      {transactions.map((tx) => {
        const config = typeConfig[tx.type];
        return (
          <Card key={tx.id}>
            <CardContent className="p-4 flex items-center justify-between">
              <div>
                <p className={cn('text-sm font-medium', config.color)}>
                  {config.label}
                </p>
                {tx.description && (
                  <p className="text-xs text-muted-foreground mt-0.5">{tx.description}</p>
                )}
                <p className="text-xs text-muted-foreground mt-0.5">
                  {formatDateTime(tx.createdDate)}
                </p>
              </div>
              <p className={cn('text-base font-semibold', config.color)}>
                {config.sign}{formatAmount(tx.amount)}원
              </p>
            </CardContent>
          </Card>
        );
      })}
      <div ref={observerRef} className="h-4" />
      {isFetchingNextPage && (
        <p className="text-center text-sm text-muted-foreground py-2">불러오는 중...</p>
      )}
    </div>
  );
}
