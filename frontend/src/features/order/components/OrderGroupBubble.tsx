'use client';

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ChevronDownIcon, ChevronUpIcon } from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { OrderCategoryGroup, OrderMenuGroup, OrderGroupMember, OrderRecord } from '../types';

interface Props {
  groups: OrderCategoryGroup[];
  maxVisibleMembers?: number;
  defaultExpanded?: boolean;
}

/**
 * 카테고리별 주문 그룹을 네스팅된 카드 형태로 표시하는 컴포넌트
 * - 카테고리 > 메뉴 > 멤버 계층 구조
 * - 한 사용자가 여러 메뉴에 중복 표시 가능
 */
export function OrderGroupBubble({
  groups,
  maxVisibleMembers = 4,
  defaultExpanded = true,
}: Props) {
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(
    defaultExpanded ? new Set(groups.map((g) => g.category)) : new Set()
  );

  const toggleCategory = (category: string) => {
    setExpandedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(category)) {
        next.delete(category);
      } else {
        next.add(category);
      }
      return next;
    });
  };

  const totalAmount = groups.reduce((sum, g) => sum + g.totalAmount, 0);

  if (groups.length === 0) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-muted-foreground">아직 주문 내역이 없어요</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {/* 전체 요약 */}
      <Card className="bg-primary/5 border-primary/20">
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">전체 주문 금액</span>
            <span className="text-xl font-bold text-foreground">
              {formatAmount(totalAmount)}원
            </span>
          </div>
        </CardContent>
      </Card>

      {/* 카테고리별 그룹 */}
      {groups.map((categoryGroup) => (
        <CategoryGroupCard
          key={categoryGroup.category}
          group={categoryGroup}
          isExpanded={expandedCategories.has(categoryGroup.category)}
          onToggle={() => toggleCategory(categoryGroup.category)}
          maxVisibleMembers={maxVisibleMembers}
        />
      ))}
    </div>
  );
}

interface CategoryGroupCardProps {
  group: OrderCategoryGroup;
  isExpanded: boolean;
  onToggle: () => void;
  maxVisibleMembers: number;
}

function CategoryGroupCard({
  group,
  isExpanded,
  onToggle,
  maxVisibleMembers,
}: CategoryGroupCardProps) {
  return (
    <Card>
      <CardHeader
        className="cursor-pointer hover:bg-accent/50 transition-colors p-4"
        onClick={onToggle}
      >
        <div className="flex items-center justify-between">
          <CardTitle className="text-base font-semibold flex items-center gap-2">
            <CategoryIcon category={group.category} />
            {group.category}
          </CardTitle>
          <div className="flex items-center gap-3">
            <span className="text-sm font-medium text-muted-foreground">
              총 {formatAmount(group.totalAmount)}원
            </span>
            {isExpanded ? (
              <ChevronUpIcon className="w-4 h-4 text-muted-foreground" />
            ) : (
              <ChevronDownIcon className="w-4 h-4 text-muted-foreground" />
            )}
          </div>
        </div>
      </CardHeader>

      {isExpanded && (
        <CardContent className="pt-0 px-4 pb-4">
          <div className="space-y-3">
            {group.menus.map((menu) => (
              <MenuGroupItem
                key={menu.menuId}
                menu={menu}
                maxVisibleMembers={maxVisibleMembers}
              />
            ))}
          </div>
        </CardContent>
      )}
    </Card>
  );
}

interface MenuGroupItemProps {
  menu: OrderMenuGroup;
  maxVisibleMembers: number;
}

function MenuGroupItem({ menu, maxVisibleMembers }: MenuGroupItemProps) {
  const [showAllMembers, setShowAllMembers] = useState(false);
  const [showOrders, setShowOrders] = useState(false);

  const visibleMembers = showAllMembers
    ? menu.members
    : menu.members.slice(0, maxVisibleMembers);
  const hiddenCount = menu.members.length - maxVisibleMembers;

  return (
    <div className="p-3 bg-secondary/30 rounded-lg">
      {/* 메뉴 정보 */}
      <div className="flex items-center justify-between mb-2">
        <span className="font-medium text-foreground">{menu.menuName}</span>
        <span className="text-sm text-muted-foreground">
          {formatAmount(menu.totalPrice)}원
        </span>
      </div>

      {/* 참여 멤버들 */}
      <div className="flex flex-wrap gap-1.5">
        {visibleMembers.map((member) => (
          <MemberChip key={member.orgMemberId} member={member} />
        ))}

        {/* 더보기 버튼 */}
        {!showAllMembers && hiddenCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            className="h-7 px-2 text-xs bg-transparent"
            onClick={() => setShowAllMembers(true)}
          >
            +{hiddenCount}
          </Button>
        )}

        {/* 접기 버튼 */}
        {showAllMembers && hiddenCount > 0 && (
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            onClick={() => setShowAllMembers(false)}
          >
            접기
          </Button>
        )}
      </div>

      {/* 주문 이력 토글 */}
      <div className="mt-2">
        <button
          className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
          onClick={() => setShowOrders((prev) => !prev)}
          aria-expanded={showOrders}
          aria-label={`${menu.menuName} 주문 이력 ${showOrders ? '접기' : '펼치기'}`}
        >
          {showOrders ? (
            <ChevronUpIcon className="w-3 h-3" />
          ) : (
            <ChevronDownIcon className="w-3 h-3" />
          )}
          주문 {menu.orders.length}건
        </button>

        {showOrders && (
          <div className="mt-2 space-y-2 pl-2 border-l-2 border-border">
            {menu.orders.map((record) => (
              <OrderRecordItem key={record.orderId} record={record} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

interface OrderRecordItemProps {
  record: OrderRecord;
}

function OrderRecordItem({ record }: OrderRecordItemProps) {
  const timeStr = new Intl.DateTimeFormat('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Asia/Seoul',
  }).format(new Date(record.createdDate));

  return (
    <div className="text-xs text-muted-foreground space-y-1">
      <div className="flex items-center justify-between">
        <span>{timeStr}</span>
        <span>{formatAmount(record.menuPrice)}원 × {record.quantity}</span>
      </div>
      <div className="flex flex-wrap gap-1">
        {record.members.map((m) => (
          <Badge
            key={m.orgMemberId}
            variant="outline"
            className="h-5 px-1.5 text-[10px] font-normal"
          >
            {m.nickname}
            {m.quantity > 1 && <span className="ml-0.5">×{m.quantity}</span>}
          </Badge>
        ))}
      </div>
    </div>
  );
}

interface MemberChipProps {
  member: OrderGroupMember;
}

function MemberChip({ member }: MemberChipProps) {
  return (
    <Badge
      variant="secondary"
      className={cn(
        'h-7 px-2.5 text-xs font-normal',
        'bg-background border border-border hover:bg-accent'
      )}
      title={`${member.nickname}: ${formatAmount(member.amount)}원`}
    >
      {member.nickname}
      {member.quantity > 1 && (
        <span className="ml-1 text-muted-foreground">×{member.quantity}</span>
      )}
    </Badge>
  );
}

function CategoryIcon({ category }: { category: string }) {
  // 카테고리별 이모지 매핑
  const iconMap: Record<string, string> = {
    메인: '🍖',
    주류: '🍺',
    음료: '🥤',
    사이드: '🍟',
    기타: '📦',
  };

  return <span>{iconMap[category] || '📦'}</span>;
}
