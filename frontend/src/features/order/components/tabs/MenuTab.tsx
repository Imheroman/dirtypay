'use client';

import { useState } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import { StoreIcon } from '@/components/common/Icons';
import { useGroupedMenusQuery } from '../../hooks/useMenusQuery';
import { MenuList } from '../MenuList';
import { CartBar } from '../CartBar';
import type { DisplayMenu, CartItem } from '../../types';

interface MenuTabProps {
  storeId: number | undefined;
  isSettled?: boolean;
  isReadOnly?: boolean;
  onAddToCart?: (items: CartItem[]) => void | Promise<void>;
  isOrderPending?: boolean;
}

const MAX_QUANTITY = 50;

export function MenuTab({ storeId, isSettled, isReadOnly, onAddToCart, isOrderPending }: MenuTabProps) {
  const [cart, setCart] = useState<Record<number, CartItem>>({});

  const { data: menusData, isLoading } =
    useGroupedMenusQuery(storeId);

  const menus: DisplayMenu[] = menusData?.all ?? [];

  const canOrder = !isReadOnly && !!onAddToCart;

  const handleAddToCart = (menu: DisplayMenu) => {
    setCart((prev) => {
      const current = prev[menu.id]?.quantity ?? 0;
      if (current >= MAX_QUANTITY) return prev;
      return {
        ...prev,
        [menu.id]: {
          menu,
          quantity: current + 1,
        },
      };
    });
  };

  const handleUpdateQuantity = (menuId: number, quantity: number) => {
    if (quantity <= 0) {
      handleRemoveFromCart(menuId);
    } else {
      setCart((prev) => ({
        ...prev,
        [menuId]: { ...prev[menuId]!, quantity: Math.min(quantity, MAX_QUANTITY) },
      }));
    }
  };

  const handleRemoveFromCart = (menuId: number) => {
    setCart((prev) => {
      const next = { ...prev };
      delete next[menuId];
      return next;
    });
  };

  const handleClearCart = () => setCart({});
  const cartItems = Object.values(cart);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="space-y-2">
          <Skeleton className="h-5 w-24" />
          <Skeleton className="h-16 w-full rounded-xl" />
          <Skeleton className="h-16 w-full rounded-xl" />
          <Skeleton className="h-16 w-full rounded-xl" />
        </div>
        <div className="space-y-2">
          <Skeleton className="h-5 w-24" />
          <Skeleton className="h-16 w-full rounded-xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <div className="flex items-center gap-2 mb-4">
          <StoreIcon className="w-5 h-5 text-primary" />
          <h3 className="font-semibold text-lg text-foreground">
            가게 메뉴판
          </h3>
        </div>
        <MenuList
          menus={menus}
          showActions={false}
          selectable={canOrder}
          onAddMenu={canOrder ? handleAddToCart : undefined}
        />
      </div>

      {/* 장바구니 */}
      <CartBar
        items={cartItems}
        onUpdateQuantity={handleUpdateQuantity}
        onRemoveItem={handleRemoveFromCart}
        onPlaceOrder={async () => {
          await onAddToCart?.(cartItems);
          handleClearCart();
        }}
        isOrdering={isOrderPending}
      />
    </div>
  );
}
