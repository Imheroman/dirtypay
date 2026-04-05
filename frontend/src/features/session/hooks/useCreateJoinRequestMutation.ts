'use client';

import { useMutation } from '@tanstack/react-query';
import { toast } from 'sonner';
import { joinRequestApi } from '../api';
import type { CreateJoinRequestPayload } from '../types';
import { isAxiosError } from 'axios';

export function useCreateJoinRequestMutation() {
  return useMutation({
    mutationFn: ({
      inviteCode,
      payload,
    }: {
      inviteCode: string;
      payload: CreateJoinRequestPayload;
    }) => joinRequestApi.create(inviteCode, payload),
    onSuccess: () => {
      toast.success('참여 요청을 보냈어요. 소유자의 승인을 기다려주세요.');
    },
    onError: (error) => {
      if (isAxiosError(error)) {
        const code = error.response?.data?.error?.code;
        switch (code) {
          case 'JOIN_002':
            toast.error('이미 대기 중인 참여 요청이 있어요.');
            return;
          case 'JOIN_004':
            toast.error('이미 세션에 참여 중이에요.');
            return;
          case 'SESSION_003':
            toast.error('이미 완료된 세션이에요.');
            return;
        }
      }
      toast.error('참여 요청을 보내지 못했어요.');
    },
  });
}
