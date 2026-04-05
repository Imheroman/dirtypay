'use client';

import { useState, useMemo } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import {
  SearchIcon,
  UserIcon,
  XIcon,
  CheckCircleIcon,
  AlertCircleIcon,
} from '@/components/common/Icons';
import { DeleteConfirmModal } from '@/components/common/ConfirmModal';
import { EmptyState } from '@/components/common/EmptyState';
import { useMembersQuery } from '../hooks/useMembersQuery';
import { useCreateMemberMutation } from '../hooks/useCreateMemberMutation';
import { useDeleteMemberMutation } from '../hooks/useDeleteMemberMutation';
import { useUpdateMemberMutation } from '../hooks/useUpdateMemberMutation';
import { useAuth } from '@/hooks/use-auth';
import type { Member } from '../types';

interface SessionMemberManagerProps {
  sessionId: number;
  ownerUserId?: number;
}

export function SessionMemberManager({
  sessionId,
  ownerUserId,
}: SessionMemberManagerProps) {
  const { user: currentUser } = useAuth();

  // 데이터 조회
  const { data: members = [], isLoading } = useMembersQuery(sessionId);

  // Mutations
  const createMutation = useCreateMemberMutation();
  const deleteMutation = useDeleteMemberMutation();
  const updateMutation = useUpdateMemberMutation();

  // UI 상태
  const [searchQuery, setSearchQuery] = useState('');
  const [newMemberName, setNewMemberName] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<Member | null>(null);

  // 클라이언트 검색 필터링
  const filteredMembers = useMemo(() => {
    if (!searchQuery.trim()) return members;
    const query = searchQuery.toLowerCase();
    return members.filter(
      (member) =>
        member.nickname.toLowerCase().includes(query) ||
        (member.userId?.toString().includes(query) ?? false)
    );
  }, [members, searchQuery]);

  // 현재 사용자 확인
  const isCurrentUser = (member: Member) => {
    return currentUser && member.userId === currentUser.id;
  };

  // 멤버 추가
  const handleAddMember = () => {
    if (!newMemberName.trim()) return;
    createMutation.mutate(
      {
        sessionId,
        request: { nickname: newMemberName },
      },
      {
        onSuccess: () => {
          setNewMemberName('');
        },
      }
    );
  };

  // 멤버 삭제
  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(
      { id: deleteTarget.id, sessionId },
      {
        onSuccess: () => {
          setDeleteTarget(null);
        },
      }
    );
  };

  // 멤버 활성 토글
  const handleToggleActive = (member: Member) => {
    updateMutation.mutate({
      id: member.id,
      sessionId,
      request: { nickname: member.nickname, isActive: !member.isActive },
    });
  };

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardContent className="p-4 flex items-center gap-3">
              <Skeleton className="h-10 w-10 rounded-full" />
              <div className="flex-1">
                <Skeleton className="h-4 w-24 mb-1" />
                <Skeleton className="h-3 w-16" />
              </div>
              <Skeleton className="h-6 w-12" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* 검색 입력 */}
      <div className="relative">
        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <Input
          placeholder="이름 또는 아이디로 검색..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* 멤버 추가 섹션 */}
      <div className="flex gap-2">
        <Input
          placeholder="새 멤버 이름"
          value={newMemberName}
          onChange={(e) => setNewMemberName(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleAddMember()}
        />
        <Button
          onClick={handleAddMember}
          disabled={!newMemberName.trim() || createMutation.isPending}
          className="whitespace-nowrap"
        >
          + 멤버 추가
        </Button>
      </div>

      {/* 멤버 목록 */}
      {filteredMembers.length > 0 ? (
        <div className="space-y-2">
          {filteredMembers.map((member) => (
            <Card key={member.id} className="hover:shadow-sm transition-shadow">
              <CardContent className="p-4 flex items-center gap-3">
                {/* 아바타 */}
                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                  <UserIcon className="w-5 h-5 text-primary" />
                </div>

                {/* 멤버 정보 */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-foreground truncate">
                      {member.nickname}
                    </p>
                    {isCurrentUser(member) && (
                      <Badge variant="secondary" className="shrink-0">
                        나
                      </Badge>
                    )}
                  </div>
                  {member.userId && (
                    <p className="text-xs text-muted-foreground">
                      ID: {member.userId}
                    </p>
                  )}
                </div>

                {/* 활성 상태 + 삭제 버튼 */}
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    onClick={() => handleToggleActive(member)}
                    className="p-1.5 rounded-lg hover:bg-accent transition-colors"
                    title={member.isActive ? '비활성화' : '활성화'}
                    disabled={updateMutation.isPending}
                  >
                    {member.isActive ? (
                      <CheckCircleIcon className="w-5 h-5 text-green-500" />
                    ) : (
                      <AlertCircleIcon className="w-5 h-5 text-muted-foreground" />
                    )}
                  </button>
                  {member.userId !== ownerUserId && (
                    <button
                      onClick={() => setDeleteTarget(member)}
                      className="p-1.5 rounded-lg hover:bg-destructive/10 transition-colors"
                      title="삭제"
                      disabled={deleteMutation.isPending}
                    >
                      <XIcon className="w-5 h-5 text-destructive" />
                    </button>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={<UserIcon className="w-6 h-6 text-muted-foreground" />}
          title="멤버가 없어요"
          description={
            searchQuery.trim()
              ? '검색 조건에 맞는 멤버가 없어요'
              : '멤버를 추가해 보세요'
          }
        />
      )}

      {/* 삭제 확인 다이얼로그 */}
      <DeleteConfirmModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        itemName={deleteTarget?.nickname}
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
