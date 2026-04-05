'use client';

import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { CheckIcon, XIcon, MessageCircleIcon, ClockIcon } from '@/components/common/Icons';
import { EmptyState } from '@/components/common/EmptyState';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { useJoinRequestsQuery } from '../hooks/useJoinRequestsQuery';
import { useApproveJoinRequestMutation } from '../hooks/useApproveJoinRequestMutation';
import { useRejectJoinRequestMutation } from '../hooks/useRejectJoinRequestMutation';
import type { JoinRequest } from '../types';

interface JoinRequestListProps {
  sessionId: number;
  hideEmpty?: boolean;
}

export function JoinRequestList({ sessionId, hideEmpty }: JoinRequestListProps) {
  const { data: requests = [], isLoading, isError } = useJoinRequestsQuery(sessionId, 'PENDING');
  const approveMutation = useApproveJoinRequestMutation();
  const rejectMutation = useRejectJoinRequestMutation();

  const [rejectTarget, setRejectTarget] = useState<JoinRequest | null>(null);

  const handleReject = () => {
    if (!rejectTarget) return;
    rejectMutation.mutate(
      { sessionId, requestId: rejectTarget.id },
      {
        onSuccess: () => setRejectTarget(null),
      }
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2].map((i) => (
          <Card key={i}>
            <CardContent className="p-4">
              <div className="h-12 rounded bg-accent animate-pulse" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <EmptyState
        icon={<XIcon className="w-6 h-6 text-destructive" />}
        title="참여 요청을 불러오지 못했어요"
        description="잠시 후 다시 시도해 주세요"
      />
    );
  }

  if (requests.length === 0) {
    if (hideEmpty) return null;
    return (
      <EmptyState
        icon={<ClockIcon className="w-6 h-6 text-muted-foreground" />}
        title="대기 중인 참여 요청이 없어요"
        description="초대 코드를 공유하면 참여 요청이 여기에 표시돼요"
      />
    );
  }

  return (
    <>
      <div className="space-y-3">
        <div className="flex items-center gap-2 mb-2">
          <h3 className="text-sm font-semibold text-foreground">참여 요청</h3>
          <Badge variant="secondary">{requests.length}건</Badge>
        </div>

        {requests.map((request) => (
          <Card key={request.id}>
            <CardContent className="p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-foreground text-sm">{request.nickname}</p>
                  {request.message && (
                    <div className="flex items-start gap-1 mt-1">
                      <MessageCircleIcon className="w-3.5 h-3.5 text-muted-foreground mt-0.5 shrink-0" />
                      <p className="text-xs text-muted-foreground line-clamp-2">
                        {request.message}
                      </p>
                    </div>
                  )}
                  <p className="text-xs text-muted-foreground mt-1">
                    {new Date(request.createdDate).toLocaleDateString('ko-KR', {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                </div>
                <div className="flex gap-2 shrink-0">
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-8 text-destructive hover:text-destructive"
                    onClick={() => setRejectTarget(request)}
                    aria-label={`${request.nickname} 참여 요청 거절`}
                  >
                    <XIcon className="w-4 h-4" />
                  </Button>
                  <Button
                    size="sm"
                    className="h-8"
                    onClick={() =>
                      approveMutation.mutate({
                        sessionId,
                        requestId: request.id,
                        payload: {},
                      })
                    }
                    disabled={approveMutation.isPending}
                    aria-label={`${request.nickname} 참여 요청 승인`}
                  >
                    {approveMutation.isPending ? '처리 중...' : (
                      <>
                        <CheckIcon className="w-4 h-4 mr-1" />
                        승인
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <ConfirmModal
        isOpen={!!rejectTarget}
        onClose={() => setRejectTarget(null)}
        onConfirm={handleReject}
        title="참여 요청 거절"
        description={
          rejectTarget
            ? `'${rejectTarget.nickname}'님의 참여 요청을 거절할까요?`
            : ''
        }
        confirmText="거절"
        variant="destructive"
        isLoading={rejectMutation.isPending}
      />
    </>
  );
}
