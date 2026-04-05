'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { PlusIcon, MinusIcon, TrashIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { SharedMenu, Menu } from '../types';

interface SharedMenuDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  groupName: string;
  currentSharedMenus: SharedMenu[];
  availableMenus: Menu[];
  onSave: (menus: SharedMenu[]) => void;
}

export function SharedMenuDialog({
  open,
  onOpenChange,
  groupName,
  currentSharedMenus,
  availableMenus,
  onSave,
}: SharedMenuDialogProps) {
  // 현재 선택된 공유 메뉴들 (수정 중인 상태)
  const [selectedMenus, setSelectedMenus] = useState<SharedMenu[]>(
    currentSharedMenus
  );

  // Dialog가 열릴 때마다 현재 공유 메뉴로 초기화
  const handleOpenChange = (isOpen: boolean) => {
    if (isOpen) {
      setSelectedMenus(currentSharedMenus);
    }
    onOpenChange(isOpen);
  };

  // 메뉴 추가
  const handleAddMenu = (menu: Menu) => {
    const existingIndex = selectedMenus.findIndex((m) => m.menuId === menu.id);
    if (existingIndex >= 0) {
      // 이미 있으면 수량 증가
      setSelectedMenus((prev) =>
        prev.map((m, i) =>
          i === existingIndex ? { ...m, quantity: m.quantity + 1 } : m
        )
      );
    } else {
      // 없으면 새로 추가
      setSelectedMenus((prev) => [
        ...prev,
        {
          menuId: menu.id,
          menuName: menu.name,
          price: menu.price,
          quantity: 1,
        },
      ]);
    }
  };

  // 수량 증가
  const handleIncreaseQuantity = (menuId: number) => {
    setSelectedMenus((prev) =>
      prev.map((m) =>
        m.menuId === menuId ? { ...m, quantity: m.quantity + 1 } : m
      )
    );
  };

  // 수량 감소
  const handleDecreaseQuantity = (menuId: number) => {
    setSelectedMenus((prev) =>
      prev
        .map((m) =>
          m.menuId === menuId ? { ...m, quantity: m.quantity - 1 } : m
        )
        .filter((m) => m.quantity > 0)
    );
  };

  // 메뉴 삭제
  const handleRemoveMenu = (menuId: number) => {
    setSelectedMenus((prev) => prev.filter((m) => m.menuId !== menuId));
  };

  // 저장
  const handleSave = () => {
    onSave(selectedMenus);
    onOpenChange(false);
  };

  // 총 금액 계산
  const totalAmount = selectedMenus.reduce(
    (sum, menu) => sum + menu.price * menu.quantity,
    0
  );

  // 선택된 메뉴 ID 목록
  const selectedMenuIds = new Set(selectedMenus.map((m) => m.menuId));

  // 메뉴를 그대로 표시 (Menu 타입에는 category가 없으므로 전체 리스트)
  const allMenus = availableMenus;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>공유 메뉴 관리</DialogTitle>
          <DialogDescription>
            {groupName} 그룹의 공유 메뉴를 설정하세요
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* 현재 선택된 공유 메뉴 */}
          {selectedMenus.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-sm font-medium text-muted-foreground">
                선택된 공유 메뉴
              </h4>
              <div className="space-y-2">
                {selectedMenus.map((menu) => (
                  <div
                    key={menu.menuId}
                    className="flex items-center justify-between p-3 bg-primary/5 rounded-lg border border-primary/20"
                  >
                    <div>
                      <p className="font-medium text-foreground">{menu.menuName}</p>
                      <p className="text-sm text-muted-foreground">
                        {formatAmount(menu.price)}원 x {menu.quantity} ={' '}
                        {formatAmount(menu.price * menu.quantity)}원
                      </p>
                    </div>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => handleDecreaseQuantity(menu.menuId)}
                      >
                        <MinusIcon className="w-4 h-4" />
                      </Button>
                      <span className="w-8 text-center font-medium">
                        {menu.quantity}
                      </span>
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => handleIncreaseQuantity(menu.menuId)}
                      >
                        <PlusIcon className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-destructive hover:text-destructive"
                        onClick={() => handleRemoveMenu(menu.menuId)}
                      >
                        <TrashIcon className="w-4 h-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
              <div className="flex justify-end pt-2 border-t">
                <span className="font-semibold text-foreground">
                  총 {formatAmount(totalAmount)}원
                </span>
              </div>
            </div>
          )}

          {/* 메뉴 선택 영역 */}
          <div className="space-y-2">
            <h4 className="text-sm font-medium text-muted-foreground">
              메뉴 추가
            </h4>
            <div className="h-[200px] overflow-y-auto rounded-md border p-2">
              <div className="space-y-1">
                {allMenus.map((menu) => {
                  const isSelected = selectedMenuIds.has(menu.id);
                  return (
                    <button
                      key={menu.id}
                      onClick={() => handleAddMenu(menu)}
                      className={cn(
                        'w-full flex items-center justify-between p-2 rounded-lg text-left transition-colors',
                        isSelected
                          ? 'bg-primary/10 hover:bg-primary/15'
                          : 'hover:bg-muted'
                      )}
                    >
                      <span className="text-sm text-foreground">
                        {menu.name}
                        {isSelected && (
                          <Badge
                            variant="outline"
                            className="ml-2 text-xs"
                          >
                            추가됨
                          </Badge>
                        )}
                      </span>
                      <span className="text-sm text-muted-foreground">
                        {formatAmount(menu.price)}원
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            취소
          </Button>
          <Button onClick={handleSave}>저장</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
