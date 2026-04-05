'use client';

import { useState } from 'react';
import { use } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { ChevronLeftIcon, UserPlusIcon } from '@/components/common/Icons';
import { useSessionQuery } from '@/features/session/hooks/useSessionQuery';
import { useRoundsQuery } from '@/features/round';
import { SessionDashboardSkeleton } from '@/features/session/components/SessionSkeleton';
import { SessionDashboard } from '@/features/session/components/SessionDashboard';
import { InviteDialog } from '@/features/session/components/InviteDialog';
import { JoinRequestBadge } from '@/features/session/components/JoinRequestBadge';
import { ErrorMessage } from '@/components/common/ErrorMessage';
import { useAuthContext } from '@/components/providers/auth-provider';

interface PageProps {
  params: Promise<{ sessionId: string }>;
}

export default function SessionDashboardPage({ params }: PageProps) {
  const { sessionId } = use(params);
  const [inviteDialogOpen, setInviteDialogOpen] = useState(false);
  const { user } = useAuthContext();

  const {
    data: session,
    isLoading: isSessionLoading,
    error: sessionError,
  } = useSessionQuery(Number(sessionId));

  const { data: rounds = [], isLoading: isRoundsLoading } = useRoundsQuery(
    Number(sessionId)
  );

  if (isSessionLoading || isRoundsLoading) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
            <div className="h-5 w-5 rounded bg-accent animate-pulse" />
            <div className="h-5 w-32 rounded bg-accent animate-pulse" />
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-lg">
          <SessionDashboardSkeleton />
        </main>
      </div>
    );
  }

  if (sessionError || !session) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <ErrorMessage message="세션 정보를 불러오지 못했어요." />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link
            href="/"
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">
            {session.title}
          </h1>
          {session.status !== 'ARCHIVED' && (
            <div className="flex items-center gap-1">
              <JoinRequestBadge sessionId={Number(sessionId)} isOwner={session.ownerId === user?.id} />
              {session.ownerId === user?.id && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 p-0"
                  aria-label="세션 초대"
                  onClick={() => setInviteDialogOpen(true)}
                >
                  <UserPlusIcon className="w-5 h-5" />
                </Button>
              )}
            </div>
          )}
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        <SessionDashboard
          session={session}
          rounds={rounds}
          sessionId={Number(sessionId)}
        />
      </main>

      <InviteDialog
        open={inviteDialogOpen}
        onOpenChange={setInviteDialogOpen}
        sessionTitle={session.title}
        inviteCode={session.inviteCode ?? ''}
      />
    </div>
  );
}
