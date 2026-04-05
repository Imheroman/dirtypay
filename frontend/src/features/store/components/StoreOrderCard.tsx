'use client';

import { useState } from 'react';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { PhoneIcon } from '@/components/common/Icons';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { formatAmount, formatDateTime } from '@/lib/format';
import { StoreOrderStatusBadge } from './StoreOrderStatusBadge';
import { useChangeStoreOrderStatusMutation } from '../hooks/useChangeStoreOrderStatusMutation';
import { useCancelStoreOrderMutation } from '../hooks/useCancelStoreOrderMutation';
import type { StoreOrder, StoreOrderStatus } from '../types';

export interface StoreOrderCardProps {
  order: StoreOrder;
  storeId: string | number;
  onStatusChange?: (newStatus: StoreOrderStatus) => void;
  onCancel?: () => void;
}

interface ActionConfig {
  label: string;
  nextStatus?: StoreOrderStatus;
  variant: 'default' | 'destructive' | 'outline';
  isCancel?: boolean;
}

const ACTION_CONFIG: Partial<Record<StoreOrderStatus, ActionConfig[]>> = {
  PENDING: [
    { label: '확인', nextStatus: 'CONFIRMED', variant: 'default' },
    { label: '취소', variant: 'destructive', isCancel: true },
  ],
  CONFIRMED: [
    { label: '완료', nextStatus: 'COMPLETED', variant: 'default' },
    { label: '취소', variant: 'destructive', isCancel: true },
  ],
};

const STATUS_CONFIRM_MESSAGES: Partial<Record<StoreOrderStatus, { title: string; description: string }>> = {
  CONFIRMED: {
    title: '주문을 확인할까요?',
    description: '주문을 확인하면 준비 중 상태가 돼요.',
  },
  COMPLETED: {
    title: '주문을 완료 처리할까요?',
    description: '완료 처리하면 되돌릴 수 없어요.',
  },
};

export function StoreOrderCard({
  order,
  storeId,
  onStatusChange,
  onCancel,
}: StoreOrderCardProps) {
  const [pendingAction, setPendingAction] = useState<ActionConfig | null>(null);

  const changeStatusMutation = useChangeStoreOrderStatusMutation(() => {
    if (pendingAction?.nextStatus) {
      onStatusChange?.(pendingAction.nextStatus);
    }
    setPendingAction(null);
  });

  const cancelMutation = useCancelStoreOrderMutation(() => {
    onCancel?.();
    setPendingAction(null);
  });

  const handleActionClick = (action: ActionConfig) => {
    setPendingAction(action);
  };

  const handleConfirm = () => {
    if (!pendingAction) return;

    if (pendingAction.isCancel) {
      cancelMutation.mutate({
        storeId: String(storeId),
        orderId: String(order.id),
      });
    } else if (pendingAction.nextStatus) {
      changeStatusMutation.mutate({
        storeId: String(storeId),
        orderId: String(order.id),
        request: { status: pendingAction.nextStatus },
      });
    }
  };

  const isMutating = changeStatusMutation.isPending || cancelMutation.isPending;
  const actions = ACTION_CONFIG[order.status] ?? [];

  const confirmModalConfig = pendingAction?.isCancel
    ? {
        title: '주문을 취소할까요?',
        description: '취소하면 되돌릴 수 없어요.',
        confirmText: '취소하기',
        variant: 'destructive' as const,
      }
    : pendingAction?.nextStatus
      ? {
          title: STATUS_CONFIRM_MESSAGES[pendingAction.nextStatus]?.title ?? '상태를 변경할까요?',
          description: STATUS_CONFIRM_MESSAGES[pendingAction.nextStatus]?.description,
          confirmText: pendingAction.label,
          variant: 'default' as const,
        }
      : null;

  return (
    <>
      <Card className="transition-shadow hover:shadow-md">
        <CardHeader className="pb-2">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <p className="text-xs text-muted-foreground">주문번호</p>
              <p className="font-mono text-sm font-medium">{order.orderNumber}</p>
            </div>
            <StoreOrderStatusBadge status={order.status} />
          </div>
        </CardHeader>

        <CardContent className="space-y-3 pb-3">
          {/* 메뉴 + 금액 */}
          <div className="space-y-1">
            <p className="font-medium">{order.menuName}</p>
            <p className="text-sm text-muted-foreground">
              {formatAmount(order.unitPrice)}원 × {order.quantity}개
              <span className="ml-2 font-semibold text-foreground">
                = {formatAmount(order.totalPrice)}원
              </span>
            </p>
          </div>

          {/* 고객 정보 */}
          {(order.customerName || order.customerPhone) && (
            <div className="space-y-1 border-t pt-2">
              {order.customerName && (
                <p className="text-sm text-muted-foreground">
                  고객: <span className="text-foreground">{order.customerName}</span>
                </p>
              )}
              {order.customerPhone && (
                <div className="flex items-center gap-1 text-sm text-muted-foreground">
                  <PhoneIcon className="h-3.5 w-3.5" aria-hidden="true" />
                  <span>{order.customerPhone}</span>
                </div>
              )}
            </div>
          )}

          {/* 생성일시 */}
          <p className="text-xs text-muted-foreground">
            {formatDateTime(order.createdDate)}
          </p>
        </CardContent>

        {actions.length > 0 && (
          <CardFooter className="gap-2 pt-0">
            {actions.map((action) => (
              <Button
                key={action.label}
                variant={action.variant}
                size="sm"
                disabled={isMutating}
                onClick={() => handleActionClick(action)}
                aria-label={`주문 ${order.orderNumber} ${action.label}`}
              >
                {action.label}
              </Button>
            ))}
          </CardFooter>
        )}
      </Card>

      {pendingAction && confirmModalConfig && (
        <ConfirmModal
          isOpen
          onClose={() => setPendingAction(null)}
          onConfirm={handleConfirm}
          title={confirmModalConfig.title}
          description={confirmModalConfig.description}
          confirmText={confirmModalConfig.confirmText}
          variant={confirmModalConfig.variant}
          isLoading={isMutating}
        />
      )}
    </>
  );
}
