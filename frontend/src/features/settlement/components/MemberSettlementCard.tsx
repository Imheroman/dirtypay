'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { UserIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { MemberAmount } from '../types';

interface MemberSettlementCardProps {
  member: MemberAmount;
  onTogglePaid?: (orgMemberId: number, paidAmount: number) => void;
  onClick?: () => void;
}

export function MemberSettlementCard({
  member,
  onTogglePaid,
  onClick,
}: MemberSettlementCardProps) {
  return (
    <Card className={onClick ? 'cursor-pointer' : ''} onClick={onClick}>
      <CardContent className="p-4">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-10 h-10 rounded-full bg-secondary flex items-center justify-center">
            <UserIcon className="w-5 h-5 text-secondary-foreground" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <span className="font-medium text-foreground truncate">
                {member.nickname}
              </span>
              {member.isExcluded && (
                <Badge variant="outline" className="text-xs shrink-0">
                  제외
                </Badge>
              )}
            </div>
            <p className="text-sm text-muted-foreground">
              남은 금액: {formatAmount(member.remainingAmount)}원
            </p>
          </div>
          <div className="text-right shrink-0">
            <p className="font-bold text-foreground">
              {formatAmount(member.amount)}원
            </p>
            <Badge
              variant={member.isPaid ? 'default' : 'secondary'}
              className="text-xs mt-1 cursor-pointer"
              onClick={(e) => {
                e.stopPropagation();
                onTogglePaid?.(
                  member.orgMemberId,
                  member.isPaid ? 0 : member.amount
                );
              }}
            >
              {member.isPaid ? '정산 완료' : '정산 대기'}
            </Badge>
          </div>
        </div>

        {/* 납부 금액 상세 */}
        {member.paidAmount > 0 && (
          <div className="pt-3 border-t border-border">
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">납부 금액</span>
              <span className="text-foreground">
                {formatAmount(member.paidAmount)}원
              </span>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
