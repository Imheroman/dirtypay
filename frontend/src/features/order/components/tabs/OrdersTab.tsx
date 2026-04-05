'use client';

import { useState } from 'react';
import { cn } from '@/lib/utils';
import { OrderGroupBubble } from '../OrderGroupBubble';
import { findMyGroup, getGroupMemberIds, filterOrdersByMembers, groupOrdersByCategory } from '../../utils';
import type { OrderCategoryGroup, OrderWithDetails, RoundGroup } from '../../types';

interface OrdersTabProps {
  orderGroups: OrderCategoryGroup[];
  allOrders?: OrderWithDetails[];
  roundGroups?: RoundGroup[];
  currentMemberId?: number;
}

export function OrdersTab({ orderGroups, allOrders = [], roundGroups = [], currentMemberId }: OrdersTabProps) {
  const [filter, setFilter] = useState<'all' | 'myGroup'>('all');

  const myGroup = findMyGroup(roundGroups, currentMemberId);

  const displayGroups = (() => {
    if (filter === 'all' || !myGroup) return orderGroups;
    const memberIds = getGroupMemberIds(myGroup);
    const filtered = filterOrdersByMembers(allOrders, memberIds);
    return groupOrdersByCategory(filtered);
  })();

  return (
    <div>
      {/* 세그먼트 컨트롤 */}
      <div className="flex rounded-lg bg-muted p-1 mb-4">
        <button
          className={cn(
            'flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
            filter === 'all'
              ? 'bg-background text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          )}
          onClick={() => setFilter('all')}
        >
          전체
        </button>
        <button
          className={cn(
            'flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
            filter === 'myGroup'
              ? 'bg-background text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground'
          )}
          onClick={() => setFilter('myGroup')}
        >
          내 그룹
        </button>
      </div>

      {/* 내 그룹 미참여 안내 */}
      {filter === 'myGroup' && !myGroup ? (
        <div className="text-center py-8">
          <p className="text-muted-foreground">
            참여 중인 그룹이 없어요. 그룹 탭에서 그룹에 참여해 보세요.
          </p>
        </div>
      ) : (
        <OrderGroupBubble groups={displayGroups} />
      )}
    </div>
  );
}
