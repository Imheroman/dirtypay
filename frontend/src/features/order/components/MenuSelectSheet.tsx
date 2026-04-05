'use client';

import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetFooter,
  SheetDescription,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { StoreIcon, EditIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { DisplayMenu } from '../types';

interface MenuSelectSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  storeMenus: DisplayMenu[];
  customMenus: DisplayMenu[];
  onAddToOrder: (menu: DisplayMenu) => void;
}

export function MenuSelectSheet({
  open,
  onOpenChange,
  storeMenus,
  customMenus,
  onAddToOrder,
}: MenuSelectSheetProps) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="bottom" className="h-[80vh]">
        <SheetHeader>
          <SheetTitle>메뉴 담기</SheetTitle>
          <SheetDescription>먹은 메뉴를 선택해주세요</SheetDescription>
        </SheetHeader>

        <div className="py-4 overflow-auto h-[calc(100%-140px)] space-y-4">
          {/* 가게 메뉴 */}
          <div>
            <p className="text-xs font-medium text-muted-foreground mb-3 flex items-center gap-1 uppercase tracking-wider">
              <StoreIcon className="w-3.5 h-3.5" /> 가게 메뉴판
            </p>
            <div className="grid grid-cols-2 gap-2">
              {storeMenus.map((item) => (
                <button
                  key={item.id}
                  onClick={() => onAddToOrder(item)}
                  className="p-3 text-left bg-card rounded-lg border border-border hover:border-primary hover:bg-primary/5 transition-colors"
                >
                  <p className="text-sm font-medium text-foreground">
                    {item.name}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatAmount(item.price)}원
                  </p>
                </button>
              ))}
            </div>
          </div>

          {/* 커스텀 메뉴 */}
          {customMenus.length > 0 && (
            <div>
              <p className="text-xs font-medium text-muted-foreground mb-3 flex items-center gap-1 uppercase tracking-wider">
                <EditIcon className="w-3.5 h-3.5" /> 커스텀 메뉴
              </p>
              <div className="grid grid-cols-2 gap-2">
                {customMenus.map((item) => (
                  <button
                    key={item.id}
                    onClick={() => onAddToOrder(item)}
                    className="p-3 text-left bg-card rounded-lg border border-border hover:border-primary hover:bg-primary/5 transition-colors"
                  >
                    <p className="text-sm font-medium text-foreground">
                      {item.name}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {formatAmount(item.price)}원
                    </p>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        <SheetFooter>
          <Button className="w-full" onClick={() => onOpenChange(false)}>
            완료
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
