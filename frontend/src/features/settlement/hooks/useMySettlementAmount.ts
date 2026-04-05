'use client';

import { useMemo } from 'react';
import { useAuthContext } from '@/components/providers/auth-provider';
import { useMembersQuery } from '@/features/organization';
import { useSessionSettlementQuery } from './useSessionSettlementQuery';

export function useMySettlementAmount(sessionId: number) {
  const { user } = useAuthContext();
  const { data: members } = useMembersQuery(sessionId);
  const { data: settlement } = useSessionSettlementQuery(sessionId);

  const { myAmount, hasSettlementData } = useMemo(() => {
    if (settlement && user && members) {
      const me = members.find(m => m.userId === user.id);
      if (me) {
        const mySettlement = settlement.settlements.find(s => s.orgMemberId === me.id);
        if (mySettlement) {
          return { myAmount: mySettlement.amount, hasSettlementData: true };
        }
      }
    }
    return { myAmount: null, hasSettlementData: false };
  }, [settlement, user, members]);

  return { myAmount, hasSettlementData };
}
