import type { RoundGroup, OrderWithDetails, OrderCategoryGroup, OrderMenuGroup, OrderRecord, OrderGroupMember } from './types';

/** 재귀적으로 사용자가 참여 중인 그룹 찾기 (isParticipating → isCurrentUser → orgMemberId 순 폴백) */
export function findMyGroup(groups: RoundGroup[], currentMemberId?: number): RoundGroup | null {
  for (const group of groups) {
    if (
      group.isParticipating ||
      group.members.some((m) => m.isCurrentUser || (currentMemberId != null && m.orgMemberId === currentMemberId))
    ) {
      return group;
    }
    const found = findMyGroup(group.childGroups, currentMemberId);
    if (found) return found;
  }
  return null;
}

/** 그룹 멤버의 orgMemberId 집합 반환 */
export function getGroupMemberIds(group: RoundGroup): Set<number> {
  const ids = new Set<number>();
  for (const member of group.members) {
    ids.add(member.orgMemberId);
  }
  return ids;
}

/** 모든 그룹(재귀)의 멤버 orgMemberId 집합 반환 */
export function getAllGroupMemberIds(groups: RoundGroup[]): Set<number> {
  const ids = new Set<number>();
  const collect = (items: RoundGroup[]) => {
    for (const g of items) {
      for (const m of g.members) ids.add(m.orgMemberId);
      collect(g.childGroups);
    }
  };
  collect(groups);
  return ids;
}

/** 멤버 ID로 주문 필터링 */
export function filterOrdersByMembers(
  orders: OrderWithDetails[],
  memberIds: Set<number>
): OrderWithDetails[] {
  return orders.filter((order) =>
    order.details.some((d) => memberIds.has(d.orgMemberId))
  );
}

/** 그룹 트리에서 현재 유저의 orgMemberId 추출 (isCurrentUser 플래그 기반) */
export function findCurrentOrgMemberId(groups: RoundGroup[]): number | undefined {
  for (const group of groups) {
    const me = group.members.find((m) => m.isCurrentUser);
    if (me) return me.orgMemberId;
    const found = findCurrentOrgMemberId(group.childGroups);
    if (found) return found;
  }
  return undefined;
}

/** 주문을 카테고리별로 그룹화 (같은 menuId 병합) */
export function groupOrdersByCategory(orders: OrderWithDetails[]): OrderCategoryGroup[] {
  const categoryMap = new Map<string, Map<number, OrderMenuGroup>>();

  for (const order of orders) {
    const category = '기타';

    if (!categoryMap.has(category)) {
      categoryMap.set(category, new Map());
    }

    const menuMap = categoryMap.get(category)!;

    // 표시용 파생값
    const menuPrice = order.quantity > 0 ? Math.round(order.totalPrice / order.quantity) : 0;
    const totalShareRatio = order.details.reduce((sum, d) => sum + d.shareRatio, 0);

    const orderMembers: OrderGroupMember[] = order.details.map((detail) => ({
      orgMemberId: detail.orgMemberId,
      nickname: detail.nickname,
      quantity: detail.shareRatio,
      amount: totalShareRatio > 0
        ? Math.round(order.totalPrice * detail.shareRatio / totalShareRatio)
        : 0,
    }));

    const record: OrderRecord = {
      orderId: order.id,
      menuPrice,
      quantity: order.quantity,
      totalPrice: order.totalPrice,
      createdDate: order.createdDate,
      members: orderMembers,
    };

    if (menuMap.has(order.menuId)) {
      // 같은 메뉴 병합
      const existing = menuMap.get(order.menuId)!;
      existing.quantity += order.quantity;
      existing.totalPrice += order.totalPrice;
      existing.orders.push(record);

      // 멤버 병합 (같은 orgMemberId면 수량/금액 합산)
      for (const newMember of orderMembers) {
        const existingMember = existing.members.find((m) => m.orgMemberId === newMember.orgMemberId);
        if (existingMember) {
          existingMember.quantity += newMember.quantity;
          existingMember.amount += newMember.amount;
        } else {
          existing.members.push({ ...newMember });
        }
      }
    } else {
      // 새 메뉴 항목
      menuMap.set(order.menuId, {
        menuId: order.menuId,
        menuName: order.menuName,
        menuPrice,
        quantity: order.quantity,
        totalPrice: order.totalPrice,
        members: orderMembers.map((m) => ({ ...m })),
        orders: [record],
      });
    }
  }

  const result: OrderCategoryGroup[] = [];
  for (const [category, menuMap] of categoryMap) {
    const menus = Array.from(menuMap.values());
    const totalAmount = menus.reduce((sum, menu) => sum + menu.totalPrice, 0);
    result.push({ category, totalAmount, menus });
  }

  return result;
}
