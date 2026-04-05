'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { groupApi } from '../api';
import type { SaveSharedMenusRequest } from '../types';

interface SaveSharedMenusParams {
  groupId: number;
  roundId: number;
  request: SaveSharedMenusRequest;
}

export function useSaveSharedMenusMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ groupId, request }: SaveSharedMenusParams) =>
      groupApi.saveSharedMenus(groupId, request),
    onSuccess: (_, { roundId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.roundGroups.byRound(String(roundId)),
      });
      toast.success('공유 메뉴를 저장했어요');
    },
    onError: (error) => {
      const message =
        error instanceof AxiosError
          ? error.response?.data?.error?.message
          : undefined;
      toast.error(message || '공유 메뉴를 저장하지 못했어요');
    },
  });
}
