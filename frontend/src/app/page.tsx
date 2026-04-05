'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  SessionList,
  SessionCreateModal,
  SessionCard,
  SessionCardSkeleton,
  JoinSessionDialog,
  useArchivedSessionsQuery,
  useDeleteSessionMutation,
} from '@/features/session';
import type { Session } from '@/features/session';
import { EmptyState } from '@/components/common/EmptyState';
import { ErrorMessage } from '@/components/common/ErrorMessage';
import { DeleteConfirmModal } from '@/components/common/ConfirmModal';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { PlusIcon, LogInIcon, UserPlusIcon, CalendarIcon } from '@/components/common/Icons';
import { useAuthContext } from '@/components/providers/auth-provider';

function ArchivedSessionList() {
  const { data: sessions, isLoading, error, refetch } = useArchivedSessionsQuery();
  const deleteMutation = useDeleteSessionMutation();
  const [deleteTarget, setDeleteTarget] = useState<Session | null>(null);

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
        message="완료된 세션 목록을 불러오지 못했어요."
        onRetry={() => refetch()}
      />
    );
  }

  if (!sessions || sessions.length === 0) {
    return (
      <EmptyState
        icon={<CalendarIcon className="h-6 w-6 text-muted-foreground" />}
        title="완료된 세션이 없어요"
        description="만료된 세션이 여기에 표시돼요"
      />
    );
  }

  return (
    <>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {sessions.map((session) => (
          <SessionCard
            key={session.id}
            session={session}
            onDelete={handleDelete}
          />
        ))}
      </div>

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

export default function Home() {
  const { isAuthenticated, user, isLoading } = useAuthContext();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [joinDialogOpen, setJoinDialogOpen] = useState(false);
  const [tab, setTab] = useState<'active' | 'archived'>('active');

  // 로딩 중일 때
  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  // 비로그인 상태일 때 보여줄 화면
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-background">
        <main className="container mx-auto px-6 py-8 max-w-5xl">
          <section className="mb-8">
            <h2 className="text-3xl font-bold text-foreground mb-2">
              Dirty Pay
            </h2>
            <p className="text-muted-foreground text-lg">
              정밀한 N/1 정산 서비스
            </p>
          </section>

          <Card className="mb-6">
            <CardHeader>
              <CardTitle>시작하기</CardTitle>
              <CardDescription>
                로그인하고 정산을 시작해보세요
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3 sm:flex-row">
              <Link href="/login" className="flex-1">
                <Button className="w-full">
                  <LogInIcon className="w-4 h-4 mr-2" />
                  로그인
                </Button>
              </Link>
              <Link href="/signup" className="flex-1">
                <Button variant="outline" className="w-full">
                  <UserPlusIcon className="w-4 h-4 mr-2" />
                  회원가입
                </Button>
              </Link>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>초대 코드로 참가하기</CardTitle>
              <CardDescription>
                초대 코드가 있다면 바로 참가할 수 있어요
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button
                className="w-full"
                variant="outline"
                onClick={() => setJoinDialogOpen(true)}
              >
                <LogInIcon className="w-4 h-4 mr-2" />
                초대 코드 입력하기
              </Button>
            </CardContent>
          </Card>

          <JoinSessionDialog
            open={joinDialogOpen}
            onOpenChange={setJoinDialogOpen}
          />
        </main>
      </div>
    );
  }

  // 로그인 상태일 때 보여줄 화면
  return (
    <div className="min-h-screen bg-background">
      <main className="container mx-auto px-6 py-8 max-w-5xl">
        <section className="mb-8">
          <h2 className="text-3xl font-bold text-foreground mb-2">
            안녕하세요{user?.name ? `, ${user.name}님` : ''}
          </h2>
          <p className="text-muted-foreground text-lg">
            정산할 모임을 선택하거나 새로 만들어보세요
          </p>
        </section>

        <section>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-muted-foreground">내 세션</h3>
            {tab === 'active' && (
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="bg-transparent"
                  onClick={() => setJoinDialogOpen(true)}
                >
                  <LogInIcon className="w-4 h-4 mr-1.5" />
                  참가하기
                </Button>
                <Button size="sm" onClick={() => setIsCreateModalOpen(true)}>
                  <PlusIcon className="w-4 h-4 mr-1.5" />
                  새로 만들기
                </Button>
              </div>
            )}
          </div>

          <div className="flex gap-4 border-b mb-4">
            <button
              onClick={() => setTab('active')}
              className={`pb-2 text-sm transition-colors ${
                tab === 'active'
                  ? 'font-semibold text-foreground border-b-2 border-primary'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              진행중
            </button>
            <button
              onClick={() => setTab('archived')}
              className={`pb-2 text-sm transition-colors ${
                tab === 'archived'
                  ? 'font-semibold text-foreground border-b-2 border-primary'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              완료
            </button>
          </div>

          {tab === 'active' ? (
            <SessionList onCreateSession={() => setIsCreateModalOpen(true)} />
          ) : (
            <ArchivedSessionList />
          )}
        </section>
      </main>

      <SessionCreateModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />

      <JoinSessionDialog
        open={joinDialogOpen}
        onOpenChange={setJoinDialogOpen}
      />

    </div>
  );
}
