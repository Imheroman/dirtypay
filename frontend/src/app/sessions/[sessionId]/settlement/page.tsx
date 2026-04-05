'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import { use } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  ChevronLeftIcon,
  ShareIcon,
} from '@/components/common/Icons';
import { ErrorMessage } from '@/components/common/ErrorMessage';
import {
  SettlementSummaryCard,
  MemberSettlementCard,
  RoundSettlementCard,
  SettlementExportDialog,
  SettlementDetailSheet,
  SettlementPageSkeleton,
  SettlementTransferSection,
} from '@/features/settlement';
import {
  useSessionSettlementQuery,
  useUpdateMemberPaymentMutation,
} from '@/features/settlement';
import { useSessionQuery } from '@/features/session';
import { useAuthContext } from '@/components/providers/auth-provider';
import { useMembersQuery } from '@/features/organization';

interface PageProps {
  params: Promise<{ sessionId: string }>;
}

export default function SettlementPage({ params }: PageProps) {
  const { sessionId } = use(params);
  const [exportDialogOpen, setExportDialogOpen] = useState(false);
  const [selectedMember, setSelectedMember] = useState<{ orgMemberId: number; nickname: string } | null>(null);

  const { data: settlement, isLoading, error, refetch } = useSessionSettlementQuery(Number(sessionId));
  const { data: session } = useSessionQuery(Number(sessionId));
  const paymentMutation = useUpdateMemberPaymentMutation(Number(sessionId));
  const { user } = useAuthContext();
  const { data: members } = useMembersQuery(Number(sessionId));

  const myOrgMember = useMemo(() => {
    if (!user || !members) return null;
    return members.find(m => m.userId === user.id) ?? null;
  }, [user, members]);

  const mySettlement = useMemo(() => {
    if (!myOrgMember || !settlement) return null;
    return settlement.settlements.find(s => s.orgMemberId === myOrgMember.id) ?? null;
  }, [myOrgMember, settlement]);

  const handleTogglePaid = (orgMemberId: number, paidAmount: number) => {
    paymentMutation.mutate({ orgMemberId, paidAmount });
  };

  // 로딩 상태
  if (isLoading) {
    return <SettlementPageSkeleton />;
  }

  // 에러 상태
  if (error || !settlement) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
            <Link
              href={`/sessions/${sessionId}`}
              className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
            >
              <ChevronLeftIcon className="w-5 h-5 text-foreground" />
            </Link>
            <h1 className="text-lg font-semibold text-foreground flex-1">
              정산 현황
            </h1>
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-lg">
          <ErrorMessage
            message="정산 정보를 불러오지 못했어요."
            onRetry={() => refetch()}
          />
        </main>
      </div>
    );
  }

  const paidAmount = settlement.settlements
    .filter((m) => m.isPaid)
    .reduce((sum, m) => sum + m.paidAmount, 0);
  const remainingAmount = settlement.totalAmount - paidAmount;

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link
            href={`/sessions/${sessionId}`}
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">
            정산 현황
          </h1>
          <Button variant="ghost" size="icon" onClick={() => setExportDialogOpen(true)}>
            <ShareIcon className="w-5 h-5" />
          </Button>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        {/* 요약 카드 */}
        <div className="mb-6">
          <SettlementSummaryCard
            totalAmount={settlement.totalAmount}
            paidAmount={paidAmount}
            remainingAmount={remainingAmount}
            roundCount={settlement.rounds.length}
            memberCount={settlement.settlements.length}
          />
        </div>

        {/* 탭 뷰 */}
        <Tabs defaultValue="members" className="w-full">
          <TabsList className="w-full mb-4 grid grid-cols-3">
            <TabsTrigger value="members">
              멤버별
            </TabsTrigger>
            <TabsTrigger value="orders">
              주문별
            </TabsTrigger>
            <TabsTrigger value="rounds">
              라운드별
            </TabsTrigger>
          </TabsList>

          {/* 멤버별 정산 */}
          <TabsContent value="members" className="space-y-3">
            {settlement.settlements.map((member) => (
              <MemberSettlementCard
                key={member.orgMemberId}
                member={member}
                onTogglePaid={handleTogglePaid}
                onClick={() => setSelectedMember({ orgMemberId: member.orgMemberId, nickname: member.nickname })}
              />
            ))}
          </TabsContent>

          {/* 주문별 정산 */}
          <TabsContent value="orders">
            <Card>
              <CardContent className="p-8 text-center">
                <p className="text-muted-foreground">
                  아직 주문별 데이터가 없어요
                </p>
              </CardContent>
            </Card>
          </TabsContent>

          {/* 라운드별 정산 */}
          <TabsContent value="rounds" className="space-y-3">
            {settlement.rounds.map((round) => (
              <RoundSettlementCard key={round.roundId} round={round} />
            ))}
          </TabsContent>
        </Tabs>

        {/* 정산 송금 섹션 */}
        {myOrgMember && mySettlement && mySettlement.remainingAmount > 0 && (
          <div className="mt-6">
            <h2 className="text-base font-semibold mb-3">정산 송금</h2>
            <SettlementTransferSection
              sessionId={Number(sessionId)}
              orgMemberId={myOrgMember.id}
              myAmount={mySettlement.remainingAmount}
              strategy={settlement.strategy}
            />
          </div>
        )}

        {/* 내보내기 버튼 */}
        <Button
          variant="outline"
          className="w-full mt-6 bg-transparent"
          onClick={() => setExportDialogOpen(true)}
        >
          정산 내역 공유하기
        </Button>
      </main>

      {/* 정산 내보내기 다이얼로그 */}
      <SettlementExportDialog
        open={exportDialogOpen}
        onOpenChange={setExportDialogOpen}
        sessionTitle={session?.title ?? '정산'}
        totalAmount={settlement.totalAmount}
        memberSettlements={settlement.settlements}
        roundSummaries={settlement.rounds}
      />

      {/* 멤버 상세 정산 시트 */}
      {selectedMember && (
        <SettlementDetailSheet
          isOpen={!!selectedMember}
          onClose={() => setSelectedMember(null)}
          sessionId={Number(sessionId)}
          orgMemberId={selectedMember.orgMemberId}
          nickname={selectedMember.nickname}
          strategy={settlement.strategy}
        />
      )}
    </div>
  );
}
