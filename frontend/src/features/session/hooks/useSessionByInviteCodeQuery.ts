'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { joinRequestApi } from '../api';

interface UseSessionByInviteCodeQueryOptions {
  enabled?: boolean;
}

export function useSessionByInviteCodeQuery(
  inviteCode: string,
  options?: UseSessionByInviteCodeQueryOptions
) {
  return useQuery({
    queryKey: queryKeys.sessions.invite(inviteCode),
    queryFn: () => joinRequestApi.lookupByInviteCode(inviteCode),
    enabled: (options?.enabled ?? true) && !!inviteCode,
    retry: false,
  });
}
