'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { MinusIcon, PlusIcon, XIcon, ChevronUpIcon, ChevronDownIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { CartItem } from '../types';

interface CartBarProps {
  items: CartItem[];
  onUpdateQuantity: (menuId: number, quantity: number) => void;
  onRemoveItem: (menuId: number) => void;
  onPlaceOrder: () => void | Promise<void>;
  isOrdering?: boolean;
  maxQuantity?: number;
}

export function CartBar({ items, onUpdateQuantity, onRemoveItem, onPlaceOrder, isOrdering, maxQuantity = 50 }: CartBarProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  if (items.length === 0) return null;

  const totalCount = items.reduce((sum, item) => sum + item.quantity, 0);
  const totalAmount = items.reduce((sum, item) => sum + item.menu.price * item.quantity, 0);

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 bg-background border-t border-border shadow-lg">
      {isExpanded && (
        <div className="max-h-64 overflow-y-auto px-4 py-3 space-y-3 container mx-auto max-w-lg">
          {items.map(({ menu, quantity }) => (
            <div key={menu.id} className="flex items-center justify-between gap-3">
              <div className="flex-1 min-w-0">
                <p className="font-medium text-sm text-foreground truncate">{menu.name}</p>
                <p className="text-xs text-muted-foreground">{formatAmount(menu.price)}원</p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <Button
                  variant="outline"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => onUpdateQuantity(menu.id, quantity - 1)}
                >
                  <MinusIcon className="w-3 h-3" />
                </Button>
                <span className="w-6 text-center text-sm font-medium">{quantity}</span>
                <Button
                  variant="outline"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => onUpdateQuantity(menu.id, quantity + 1)}
                  disabled={quantity >= maxQuantity}
                >
                  <PlusIcon className="w-3 h-3" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-destructive hover:text-destructive"
                  onClick={() => onRemoveItem(menu.id)}
                >
                  <XIcon className="w-3 h-3" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="container mx-auto max-w-lg px-4 py-3 flex items-center justify-between gap-3">
        <button
          className="flex items-center gap-2 text-sm text-muted-foreground"
          onClick={() => setIsExpanded((prev) => !prev)}
          aria-expanded={isExpanded}
          aria-label="장바구니 상세 보기"
        >
          {isExpanded ? (
            <ChevronDownIcon className="w-4 h-4" />
          ) : (
            <ChevronUpIcon className="w-4 h-4" />
          )}
          <span className="font-medium text-foreground">{totalCount}개</span>
        </button>
        <Button
          className="shrink-0"
          onClick={onPlaceOrder}
          disabled={items.length === 0 || isOrdering}
        >
          {isOrdering ? '주문 중...' : `${formatAmount(totalAmount)}원 주문`}
        </Button>
      </div>
    </div>
  );
}
