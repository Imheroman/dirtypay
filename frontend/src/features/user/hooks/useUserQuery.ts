'use client';

import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { userApi } from '../api';

export function useUserQuery(id: number) {
  return useQuery({
    queryKey: queryKeys.users.detail(id),
    queryFn: () => userApi.getUser(id),
    enabled: id > 0, // id가 유효한 경우에만 쿼리 실행
  });
}
