'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { UsersIcon, CalendarIcon, CheckCircleIcon } from '@/components/common/Icons';
import { useSessionByInviteCodeQuery } from '../hooks/useSessionByInviteCodeQuery';
import { useCreateJoinRequestMutation } from '../hooks/useCreateJoinRequestMutation';

interface JoinSessionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function JoinSessionDialog({ open, onOpenChange }: JoinSessionDialogProps) {
  const [step, setStep] = useState<'code' | 'form' | 'done'>('code');
  const [inviteCode, setInviteCode] = useState('');
  const [nickname, setNickname] = useState('');
  const [message, setMessage] = useState('');

  const sessionQuery = useSessionByInviteCodeQuery(inviteCode, {
    enabled: false,
  });
  const createJoinRequest = useCreateJoinRequestMutation();

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      setStep('code');
      setInviteCode('');
      setNickname('');
      setMessage('');
    }
    onOpenChange(nextOpen);
  };

  const handleLookup = async () => {
    if (!inviteCode.trim()) return;
    const result = await sessionQuery.refetch();
    if (result.data) {
      setStep('form');
    }
  };

  const handleSubmit = () => {
    if (!nickname.trim()) return;
    createJoinRequest.mutate(
      {
        inviteCode: inviteCode.trim(),
        payload: {
          nickname: nickname.trim(),
          message: message.trim() || undefined,
        },
      },
      {
        onSuccess: () => setStep('done'),
      }
    );
  };

  const session = sessionQuery.data;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        {step === 'code' && (
          <>
            <DialogHeader>
              <DialogTitle>세션에 참가하기</DialogTitle>
              <DialogDescription>
                초대 코드를 입력하여 세션을 찾아보세요
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <Input
                placeholder="초대 코드 입력 (예: BUSAN2024)"
                value={inviteCode}
                onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
                onKeyDown={(e) => e.key === 'Enter' && handleLookup()}
                className="font-mono text-center text-lg tracking-wider"
              />
              {sessionQuery.isError && (
                <p className="text-sm text-destructive text-center">
                  유효하지 않은 초대 코드예요
                </p>
              )}
              <p className="text-xs text-muted-foreground text-center">
                초대 코드는 세션 관리자에게 받을 수 있어요
              </p>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => handleOpenChange(false)}>
                취소
              </Button>
              <Button
                onClick={handleLookup}
                disabled={!inviteCode.trim() || sessionQuery.isFetching}
              >
                {sessionQuery.isFetching ? '조회 중...' : '조회'}
              </Button>
            </DialogFooter>
          </>
        )}

        {step === 'form' && session && (
          <>
            <DialogHeader>
              <DialogTitle>세션 참여</DialogTitle>
              <DialogDescription>
                세션 정보를 확인하고 참여 정보를 입력하세요
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <div className="rounded-lg border p-3 space-y-2">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-semibold text-foreground">{session.title}</p>
                    {session.description && (
                      <p className="text-sm text-muted-foreground mt-0.5">
                        {session.description}
                      </p>
                    )}
                  </div>
                  <Badge variant={session.status === 'ACTIVE' ? 'default' : 'secondary'}>
                    {session.status === 'ACTIVE' ? '진행 중' : '종료'}
                  </Badge>
                </div>
                <div className="flex items-center gap-4 text-sm text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <UsersIcon className="w-4 h-4" />
                    {session.memberCount ?? 0}명
                  </span>
                  {session.startDate && (
                    <span className="flex items-center gap-1">
                      <CalendarIcon className="w-4 h-4" />
                      {new Date(session.startDate).toLocaleDateString('ko-KR')}
                    </span>
                  )}
                </div>
              </div>

              <div className="space-y-2">
                <label htmlFor="join-nickname" className="text-sm font-medium text-foreground">
                  닉네임
                </label>
                <Input
                  id="join-nickname"
                  placeholder="세션에서 사용할 닉네임"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  maxLength={20}
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="join-message" className="text-sm font-medium text-foreground">
                  메시지 <span className="text-muted-foreground font-normal">(선택)</span>
                </label>
                <Input
                  id="join-message"
                  placeholder="소유자에게 보낼 메시지"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  maxLength={100}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setStep('code')}>
                뒤로
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={!nickname.trim() || createJoinRequest.isPending}
              >
                {createJoinRequest.isPending ? '요청 중...' : '참여 요청 보내기'}
              </Button>
            </DialogFooter>
          </>
        )}

        {step === 'done' && (
          <>
            <DialogHeader>
              <DialogTitle className="sr-only">참여 요청 완료</DialogTitle>
            </DialogHeader>
            <div className="py-6 text-center space-y-3">
              <div className="mx-auto w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
                <CheckCircleIcon className="w-6 h-6 text-primary" />
              </div>
              <div>
                <p className="font-semibold text-foreground">요청을 보냈어요</p>
                <p className="text-sm text-muted-foreground mt-1">
                  소유자가 승인하면 세션에 참여할 수 있어요
                </p>
              </div>
            </div>
            <DialogFooter>
              <Button onClick={() => handleOpenChange(false)}>확인</Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
