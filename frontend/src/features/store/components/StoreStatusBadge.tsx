'use client';

import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { StoreStatus } from '../types';

interface StoreStatusBadgeProps {
  status: StoreStatus;
  className?: string;
}

const STATUS_CONFIG: Record<
  StoreStatus,
  { label: string; className: string }
> = {
  ACTIVE: {
    label: '영업중',
    className:
      'border-transparent bg-green-500 text-white dark:bg-green-600',
  },
  INACTIVE: {
    label: '휴무중',
    className: '',
  },
  CLOSED: {
    label: '폐점',
    className: '',
  },
};

export function StoreStatusBadge({ status, className }: StoreStatusBadgeProps) {
  const config = STATUS_CONFIG[status];

  const variant =
    status === 'ACTIVE'
      ? 'default'
      : status === 'CLOSED'
        ? 'destructive'
        : 'secondary';

  return (
    <Badge
      variant={variant}
      className={cn(config.className, className)}
      aria-label={`매장 상태: ${config.label}`}
    >
      {config.label}
    </Badge>
  );
}
