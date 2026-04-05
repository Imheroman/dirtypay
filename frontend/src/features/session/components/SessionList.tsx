'use client';

import { useState } from 'react';
import { SessionCard } from './SessionCard';
import { SessionCardSkeleton } from './SessionSkeleton';
import { useSessionsQuery } from '../hooks/useSessionsQuery';
import { useArchiveSessionMutation } from '../hooks/useArchiveSessionMutation';
import { useDeleteSessionMutation } from '../hooks/useDeleteSessionMutation';
import { useAuthContext } from '@/components/providers/auth-provider';
import { EmptyState } from '@/components/common/EmptyState';
import { ErrorMessage } from '@/components/common/ErrorMessage';
import { ConfirmModal, DeleteConfirmModal } from '@/components/common/ConfirmModal';
import { CalendarIcon } from '@/components/common/Icons';
import type { Session } from '../types';

interface SessionListProps {
  onCreateSession?: () => void;
}

export function SessionList({ onCreateSession }: SessionListProps) {
  const { user } = useAuthContext();
  const { data: sessions, isLoading, error, refetch } = useSessionsQuery();
  const archiveMutation = useArchiveSessionMutation();
  const deleteMutation = useDeleteSessionMutation();
  const [deleteTarget, setDeleteTarget] = useState<Session | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<Session | null>(null);

  // 세션 분리: 내가 만든 세션 vs 참여 중인 세션
  const ownedSessions = sessions?.filter(s => s.ownerId === user?.id) ?? [];
  const participatingSessions = sessions?.filter(s => s.ownerId !== user?.id) ?? [];

  const handleArchive = (session: Session) => {
    setArchiveTarget(session);
  };

  const handleArchiveConfirm = () => {
    if (!archiveTarget) return;
    archiveMutation.mutate(archiveTarget.id, {
      onSuccess: () => setArchiveTarget(null),
    });
  };

  const handleDelete = (session: Session) => {
    setDeleteTarget(session);
  };

  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {[1, 2, 3].map((i) => (
          <SessionCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <ErrorMessage
        message="세션 목록을 불러오지 못했어요."
        onRetry={() => refetch()}
      />
    );
  }

  if (!sessions || (ownedSessions.length === 0 && participatingSessions.length === 0)) {
    return (
      <EmptyState
        icon={<CalendarIcon className="h-6 w-6 text-muted-foreground" />}
        title="아직 등록된 세션이 없어요"
        description="새 세션을 만들어 정산을 시작해보세요"
        action={
          onCreateSession
            ? { label: '새 세션 만들기', onClick: onCreateSession }
            : undefined
        }
      />
    );
  }

  return (
    <>
      {/* 내가 만든 세션 */}
      {ownedSessions.length > 0 && (
        <section className="mb-8">
          <h3 className="text-sm font-semibold text-foreground mb-4">내가 만든 세션</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {ownedSessions.map((session) => (
              <SessionCard
                key={session.id}
                session={session}
                onArchive={handleArchive}
                onDelete={handleDelete}
              />
            ))}
          </div>
        </section>
      )}

      {/* 참여 중인 세션 */}
      {participatingSessions.length > 0 && (
        <section className="mb-8">
          <h3 className="text-sm font-semibold text-foreground mb-4">참여 중인 세션</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {participatingSessions.map((session) => (
              <SessionCard
                key={session.id}
                session={session}
                onArchive={handleArchive}
                onDelete={handleDelete}
              />
            ))}
          </div>
        </section>
      )}

      <ConfirmModal
        isOpen={!!archiveTarget}
        onClose={() => setArchiveTarget(null)}
        onConfirm={handleArchiveConfirm}
        title="정말 세션을 마무리할까요?"
        description="마무리하면 라운드 추가/수정/삭제가 더 이상 불가해요."
        confirmText="마무리"
        isLoading={archiveMutation.isPending}
      />

      <DeleteConfirmModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        itemName={deleteTarget?.title}
        isLoading={deleteMutation.isPending}
      />
    </>
  );
}
