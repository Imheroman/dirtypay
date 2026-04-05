'use client';

import Link from 'next/link';
import { use } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ChevronLeftIcon } from '@/components/common/Icons';
import { SessionMemberManager } from '@/features/organization/components/SessionMemberManager';
import { JoinRequestList } from '@/features/session/components/JoinRequestList';
import { useSessionQuery } from '@/features/session/hooks/useSessionQuery';
import { useMembersQuery } from '@/features/organization/hooks/useMembersQuery';
import { useAuthContext } from '@/components/providers/auth-provider';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/common/ErrorMessage';

interface PageProps {
  params: Promise<{ sessionId: string }>;
}

export default function OrganizationPage({ params }: PageProps) {
  const { sessionId } = use(params);
  const { user } = useAuthContext();

  const {
    data: session,
    isLoading: isSessionLoading,
    error: sessionError,
  } = useSessionQuery(Number(sessionId));

  const isOwner = session?.ownerId === user?.id;

  const {
    data: members = [],
    isLoading: isMembersLoading,
    error: membersError,
  } = useMembersQuery(Number(sessionId), { enabled: isOwner });

  if (!isSessionLoading && session && !isOwner) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <ErrorMessage message="접근 권한이 없어요." />
      </div>
    );
  }

  if (isSessionLoading || isMembersLoading) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-2xl">
            <div className="h-5 w-5 rounded bg-accent animate-pulse" />
            <Skeleton className="h-5 w-24" />
            <div className="flex-1" />
            <Skeleton className="h-6 w-12 rounded-full" />
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-2xl space-y-6">
          <div className="grid grid-cols-2 gap-3">
            {[1, 2].map((i) => (
              <Card key={i}>
                <CardContent className="p-4 flex items-center gap-3">
                  <Skeleton className="h-10 w-10 rounded-full" />
                  <div>
                    <Skeleton className="h-4 w-16 mb-1" />
                    <Skeleton className="h-3 w-12" />
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
          <Skeleton className="h-16 w-full rounded-lg" />
          <Skeleton className="h-48 w-full rounded-lg" />
        </main>
      </div>
    );
  }

  if (sessionError || membersError) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <ErrorMessage message="세션 정보를 불러오지 못했어요." />
      </div>
    );
  }

  const totalMembers = members.length;

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-2xl">
          <Link
            href={`/sessions/${sessionId}`}
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">세션 멤버 관리</h1>
          <Badge variant="secondary">{totalMembers}명</Badge>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-2xl space-y-6">
        {session?.status !== 'ARCHIVED' && (
          <JoinRequestList sessionId={Number(sessionId)} />
        )}
        <SessionMemberManager sessionId={Number(sessionId)} ownerUserId={session?.ownerId} />
      </main>
    </div>
  );
}
