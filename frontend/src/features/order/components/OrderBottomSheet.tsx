'use client';

import { useState } from 'react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  PlusIcon,
  MinusIcon,
  CheckIcon,
  UserIcon,
} from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { Menu } from '../types';

interface Member {
  id: number;
  name: string;
}

interface OrderBottomSheetProps {
  isOpen: boolean;
  onClose: () => void;
  menu: Menu | null;
  members: Member[];
  onSubmit: (data: {
    menuId: number;
    quantity: number;
    memberIds: number[];
  }) => void;
}

export function OrderBottomSheet({
  isOpen,
  onClose,
  menu,
  members,
  onSubmit,
}: OrderBottomSheetProps) {
  const [quantity, setQuantity] = useState(1);
  const [selectedMemberIds, setSelectedMemberIds] = useState<number[]>([]);

  const handleSubmit = () => {
    if (!menu || selectedMemberIds.length === 0) return;

    onSubmit({
      menuId: menu.id,
      quantity,
      memberIds: selectedMemberIds,
    });

    handleClose();
  };

  const handleClose = () => {
    setQuantity(1);
    setSelectedMemberIds([]);
    onClose();
  };

  const toggleMember = (memberId: number) => {
    setSelectedMemberIds((prev) =>
      prev.includes(memberId)
        ? prev.filter((id) => id !== memberId)
        : [...prev, memberId]
    );
  };

  const selectAllMembers = () => {
    setSelectedMemberIds(members.map((m) => m.id));
  };

  const clearSelection = () => {
    setSelectedMemberIds([]);
  };

  const totalPrice = menu ? menu.price * quantity : 0;
  const pricePerPerson =
    selectedMemberIds.length > 0
      ? Math.floor(totalPrice / selectedMemberIds.length)
      : 0;

  if (!menu) return null;

  return (
    <Sheet open={isOpen} onOpenChange={handleClose}>
      <SheetContent side="bottom" className="h-[85vh]">
        <SheetHeader>
          <SheetTitle>{menu.name}</SheetTitle>
          <SheetDescription>
            주문 수량과 함께 먹은 사람을 선택해주세요
          </SheetDescription>
        </SheetHeader>

        <div className="py-6 space-y-6 overflow-auto h-[calc(100%-200px)]">
          {/* 메뉴 정보 & 수량 */}
          <div className="flex items-center justify-between p-4 bg-muted rounded-xl">
            <div>
              <p className="font-medium text-foreground">{menu.name}</p>
              <p className="text-sm text-muted-foreground">
                {formatAmount(menu.price)}원
              </p>
            </div>
            <div className="flex items-center gap-3">
              <Button
                variant="outline"
                size="icon"
                className="h-11 w-11 bg-background"
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
              >
                <MinusIcon className="w-4 h-4" />
              </Button>
              <span className="w-12 text-center text-xl font-bold">
                {quantity}
              </span>
              <Button
                variant="outline"
                size="icon"
                className="h-11 w-11 bg-background"
                onClick={() => setQuantity(quantity + 1)}
              >
                <PlusIcon className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* 총 금액 표시 */}
          <div className="flex items-center justify-between px-1">
            <span className="text-muted-foreground">총 금액</span>
            <span className="text-xl font-bold text-foreground">
              {formatAmount(totalPrice)}원
            </span>
          </div>

          {/* 참여자 선택 */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm font-medium text-foreground">
                함께 먹은 사람
              </p>
              <div className="flex gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-xs h-7"
                  onClick={selectAllMembers}
                >
                  전체 선택
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-xs h-7"
                  onClick={clearSelection}
                >
                  선택 해제
                </Button>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-2">
              {members.map((member) => {
                const isSelected = selectedMemberIds.includes(member.id);
                return (
                  <button
                    key={member.id}
                    role="checkbox"
                    aria-checked={isSelected}
                    onClick={() => toggleMember(member.id)}
                    className={cn(
                      'flex items-center gap-2 p-3 rounded-xl border-2 transition-colors',
                      isSelected
                        ? 'border-primary bg-primary/5'
                        : 'border-border bg-card hover:border-primary/30'
                    )}
                  >
                    <div
                      className={cn(
                        'w-8 h-8 rounded-full flex items-center justify-center shrink-0',
                        isSelected
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-secondary text-secondary-foreground'
                      )}
                    >
                      {isSelected ? (
                        <CheckIcon className="w-4 h-4" />
                      ) : (
                        <UserIcon className="w-4 h-4" />
                      )}
                    </div>
                    <span
                      className={cn(
                        'text-sm truncate',
                        isSelected
                          ? 'font-medium text-foreground'
                          : 'text-muted-foreground'
                      )}
                    >
                      {member.name}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* 1인당 금액 안내 */}
          {selectedMemberIds.length > 0 && (
            <div className="p-4 bg-primary/5 rounded-xl border border-primary/20">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">
                    {selectedMemberIds.length}명 선택
                  </p>
                  <p className="text-sm text-muted-foreground">1인당 금액</p>
                </div>
                <p className="text-xl font-bold text-primary">
                  {formatAmount(pricePerPerson)}원
                </p>
              </div>
            </div>
          )}
        </div>

        <SheetFooter className="gap-2">
          <Button
            variant="outline"
            className="flex-1 bg-transparent"
            onClick={handleClose}
          >
            취소
          </Button>
          <Button
            className="flex-1"
            onClick={handleSubmit}
            disabled={selectedMemberIds.length === 0}
          >
            주문 추가
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
