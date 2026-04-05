'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { queryKeys } from '@/hooks/queries/keys';
import { userApi } from '../api';

export function useDeleteUserMutation() {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => userApi.deleteUser(id),
    onSuccess: async (_, deletedId) => {
      // 1. 해당 사용자 쿼리 캐시 제거
      queryClient.removeQueries({ queryKey: queryKeys.users.detail(deletedId) });
      toast.success('회원 탈퇴가 완료됐어요');

      // 2. 로그아웃 처리 (자기 계정 삭제 시)
      // 서버 사이드 세션 삭제
      await fetch('/api/auth/logout', { method: 'POST' });
      queryClient.clear();
      router.push('/login');
    },
    onError: () => {
      toast.error('회원 탈퇴를 처리하지 못했어요');
    },
  });
}
