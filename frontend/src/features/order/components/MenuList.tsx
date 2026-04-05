'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EditIcon, TrashIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { DisplayMenu } from '../types';

interface MenuListProps {
  menus: DisplayMenu[];
  onAddMenu?: (menu: DisplayMenu) => void;
  onEditMenu?: (menu: DisplayMenu) => void;
  onDeleteMenu?: (menuId: number) => void;
  showActions?: boolean;
  selectable?: boolean;
}

export function MenuList({
  menus,
  onAddMenu,
  onEditMenu,
  onDeleteMenu,
  showActions = false,
  selectable = true,
}: MenuListProps) {
  const groupedMenus = menus.reduce(
    (acc, menu) => {
      const category = menu.category ?? '기타';
      if (!acc[category]) acc[category] = [];
      acc[category].push(menu);
      return acc;
    },
    {} as Record<string, DisplayMenu[]>
  );

  if (menus.length === 0) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-muted-foreground">등록된 메뉴가 없어요</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-5">
      {Object.entries(groupedMenus).map(([category, categoryMenus]) => (
        <div key={category}>
          <p className="text-xs font-medium text-muted-foreground mb-2 uppercase tracking-wider">
            {category}
          </p>
          <div className="space-y-2">
            {categoryMenus.map((menu) => (
              <div
                key={menu.id}
                className="flex items-center justify-between p-4 bg-card rounded-xl border border-border hover:border-primary/30 transition-colors"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-foreground truncate">
                      {menu.name}
                    </p>
                    {/* 커스텀 뱃지 - 백엔드 isCustom 필드 제거됨 */}
                  </div>
                  <p className="text-sm text-muted-foreground">
                    {formatAmount(menu.price)}원
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  {showActions && (
                    <>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => onEditMenu?.(menu)}
                      >
                        <EditIcon className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-destructive hover:text-destructive"
                        onClick={() => onDeleteMenu?.(menu.id)}
                      >
                        <TrashIcon className="w-4 h-4" />
                      </Button>
                    </>
                  )}
                  {selectable && onAddMenu && (
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-9 bg-transparent"
                      onClick={() => onAddMenu(menu)}
                    >
                      담기
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
