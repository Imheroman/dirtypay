'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { userApi } from '../api';
import type { UpdateUserRequest } from '../types';

interface UpdateUserParams {
  id: number;
  request: UpdateUserRequest;
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: UpdateUserParams) =>
      userApi.updateUser(id, request),
    onSuccess: (_, { id }) => {
      // 해당 사용자 쿼리 캐시 무효화
      // 현재 로그인한 사용자 정보 갱신은 호출하는 컴포넌트에서 refresh() 호출로 처리
      queryClient.invalidateQueries({ queryKey: queryKeys.users.detail(id) });
      toast.success('회원 정보를 수정했어요');
    },
    onError: () => {
      toast.error('회원 정보를 수정하지 못했어요');
    },
  });
}
