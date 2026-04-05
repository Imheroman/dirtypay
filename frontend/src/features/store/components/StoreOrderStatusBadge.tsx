'use client';

import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { StoreOrderStatus } from '../types';

export interface StoreOrderStatusBadgeProps {
  status: StoreOrderStatus;
  className?: string;
}

const ORDER_STATUS_CONFIG: Record<
  StoreOrderStatus,
  { label: string; className: string }
> = {
  PENDING: {
    label: '대기 중',
    className: 'border-transparent bg-yellow-500 text-white dark:bg-yellow-600',
  },
  CONFIRMED: {
    label: '확인됨',
    className: 'border-transparent bg-blue-500 text-white dark:bg-blue-600',
  },
  COMPLETED: {
    label: '완료',
    className: 'border-transparent bg-green-500 text-white dark:bg-green-600',
  },
  CANCELLED: {
    label: '취소됨',
    className: 'border-transparent bg-red-500 text-white dark:bg-red-600',
  },
};

export function StoreOrderStatusBadge({
  status,
  className,
}: StoreOrderStatusBadgeProps) {
  const config = ORDER_STATUS_CONFIG[status];

  return (
    <Badge
      className={cn(config.className, className)}
      aria-label={`주문 상태: ${config.label}`}
    >
      {config.label}
    </Badge>
  );
}
