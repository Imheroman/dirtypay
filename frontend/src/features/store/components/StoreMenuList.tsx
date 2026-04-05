'use client';

import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { EditIcon, TrashIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { StoreMenu } from '../types';

export interface StoreMenuListProps {
  menus: StoreMenu[];
  isLoading?: boolean;
  onToggleAvailability: (menuId: number) => void;
  onEdit: (menu: StoreMenu) => void;
  onDelete: (menu: StoreMenu) => void;
}

function groupMenusByCategory(menus: StoreMenu[]): Map<string, StoreMenu[]> {
  const grouped = new Map<string, StoreMenu[]>();

  for (const menu of menus) {
    const category = menu.category ?? '기타';
    const existing = grouped.get(category) ?? [];
    grouped.set(category, [...existing, menu]);
  }

  return grouped;
}

function MenuSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 3 }).map((_, i) => (
        <div
          key={i}
          className="flex items-center justify-between rounded-lg border p-4"
        >
          <div className="space-y-2">
            <Skeleton className="h-4 w-32" />
            <Skeleton className="h-3 w-20" />
          </div>
          <Skeleton className="h-6 w-10 rounded-full" />
        </div>
      ))}
    </div>
  );
}

interface MenuItemProps {
  menu: StoreMenu;
  onToggleAvailability: (menuId: number) => void;
  onEdit: (menu: StoreMenu) => void;
  onDelete: (menu: StoreMenu) => void;
}

function MenuItem({
  menu,
  onToggleAvailability,
  onEdit,
  onDelete,
}: MenuItemProps) {
  return (
    <div className="flex items-start justify-between gap-3 rounded-lg border p-4 transition-colors hover:bg-muted/30">
      <div className="min-w-0 flex-1 space-y-1">
        <div className="flex items-center gap-2">
          <span className="font-medium">{menu.name}</span>
          {!menu.available && (
            <span className="text-xs text-muted-foreground">(판매 중지)</span>
          )}
        </div>
        {menu.description && (
          <p className="truncate text-sm text-muted-foreground">
            {menu.description}
          </p>
        )}
        <p className="text-sm font-semibold">{formatAmount(menu.price)}원</p>
      </div>

      <div className="flex shrink-0 items-center gap-1">
        <Switch
          checked={menu.available}
          onCheckedChange={() => onToggleAvailability(menu.id)}
          aria-label={`${menu.name} 판매 ${menu.available ? '중지' : '재개'}`}
        />
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onEdit(menu)}
          aria-label={`${menu.name} 수정`}
          className="h-8 w-8 p-0 text-muted-foreground hover:text-foreground"
        >
          <EditIcon className="h-3.5 w-3.5" aria-hidden="true" />
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onDelete(menu)}
          aria-label={`${menu.name} 삭제`}
          className="h-8 w-8 p-0 text-muted-foreground hover:text-destructive"
        >
          <TrashIcon className="h-3.5 w-3.5" aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}

export function StoreMenuList({
  menus,
  isLoading = false,
  onToggleAvailability,
  onEdit,
  onDelete,
}: StoreMenuListProps) {
  if (isLoading) {
    return <MenuSkeleton />;
  }

  if (menus.length === 0) {
    return (
      <div className="py-12 text-center">
        <p className="text-sm text-muted-foreground">
          아직 등록된 메뉴가 없어요
        </p>
      </div>
    );
  }

  const grouped = groupMenusByCategory(menus);

  return (
    <div className="space-y-6">
      {Array.from(grouped.entries()).map(([category, categoryMenus]) => (
        <section key={category} aria-label={`${category} 카테고리`}>
          <h4 className="mb-2 text-sm font-semibold text-muted-foreground">
            {category}
          </h4>
          <div className="space-y-2">
            {categoryMenus.map((menu) => (
              <MenuItem
                key={menu.id}
                menu={menu}
                onToggleAvailability={onToggleAvailability}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
