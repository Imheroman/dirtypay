'use client';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { UserIcon, UsersIcon, CheckIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { RoundGroup } from '../types';

interface GroupJoinDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  group: RoundGroup | null;
  currentMemberId: number;
  onConfirm: (groupId: number, memberId: number) => void;
  currentGroupName?: string;
}

export function GroupJoinDialog({
  open,
  onOpenChange,
  group,
  currentMemberId,
  onConfirm,
  currentGroupName,
}: GroupJoinDialogProps) {
  if (!group) return null;

  const handleConfirm = () => {
    onConfirm(group.groupId, currentMemberId);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <UsersIcon className="w-5 h-5 text-primary" />
            그룹 참여하기
          </DialogTitle>
          <DialogDescription>
            {currentGroupName
              ? `현재 '${currentGroupName}' 그룹에서 나가고, 아래 그룹으로 변경할까요?`
              : '아래 그룹에 참여하시겠어요?'}
          </DialogDescription>
        </DialogHeader>

        <div className="py-4">
          {/* 그룹 정보 카드 */}
          <div className="p-4 rounded-xl border border-primary/20 bg-primary/5">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-lg text-foreground">{group.groupName}</h3>
              <Badge variant="default">{group.members.length}명</Badge>
            </div>

            {/* 현재 멤버 목록 */}
            <div className="mb-3">
              <p className="text-xs text-muted-foreground mb-2">현재 멤버</p>
              <div className="flex flex-wrap gap-2">
                {group.members.slice(0, 5).map((member) => (
                  <div
                    key={member.orgMemberId}
                    className={cn(
                      'inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-xs',
                      member.isCurrentUser
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-secondary text-secondary-foreground'
                    )}
                  >
                    <UserIcon className="w-3 h-3" />
                    {member.nickname}
                    {member.isCurrentUser && ' (나)'}
                  </div>
                ))}
                {group.members.length > 5 && (
                  <span className="text-xs text-muted-foreground">
                    +{group.members.length - 5}명
                  </span>
                )}
              </div>
            </div>

            {/* 공유 메뉴 */}
            {group.sharedMenus.length > 0 && (
              <div className="mb-3">
                <p className="text-xs text-muted-foreground mb-2">공유 메뉴</p>
                <div className="flex flex-wrap gap-2">
                  {group.sharedMenus.map((menu) => (
                    <Badge key={menu.menuId} variant="secondary" className="text-xs font-normal">
                      {menu.menuName} x{menu.quantity}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* 총액 */}
            <div className="pt-3 border-t border-border">
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">그룹 총 주문액</span>
                <span className="font-semibold text-foreground">
                  {formatAmount(group.totalAmount)}원
                </span>
              </div>
            </div>
          </div>

          {/* 참여 안내 */}
          <div className="mt-4 p-3 rounded-lg bg-muted/50">
            <div className="flex items-start gap-2">
              <CheckIcon className="w-4 h-4 text-primary mt-0.5 shrink-0" />
              <p className="text-sm text-muted-foreground">
                그룹에 참여하면 공유 메뉴 비용이 자동으로 분배되어 정산됩니다
              </p>
            </div>
            {currentGroupName && (
              <div className="flex items-start gap-2 mt-2">
                <CheckIcon className="w-4 h-4 text-primary mt-0.5 shrink-0" />
                <p className="text-sm text-muted-foreground">
                  &apos;{currentGroupName}&apos; 그룹에서 자동으로 나가게 돼요
                </p>
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" className="bg-transparent" onClick={() => onOpenChange(false)}>
            취소
          </Button>
          <Button onClick={handleConfirm}>
            <UserIcon className="w-4 h-4 mr-2" />
            {currentGroupName ? '그룹 변경하기' : '참여하기'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
