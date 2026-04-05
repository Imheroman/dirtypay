'use client';

import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  PlusIcon,
  ClockIcon,
} from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import { EmptyState } from '@/components/common/EmptyState';
import { ConfirmModal, DeleteConfirmModal } from '@/components/common/ConfirmModal';
import type { Session } from '../types';
import {
  RoundCreateModal,
  RoundTimeline,
  useCreateRoundMutation,
  useRoundManagement,
} from '@/features/round';
import type { Round } from '@/features/round';
import { useMySettlementAmount } from '@/features/settlement';

interface SessionDashboardProps {
  session: Session;
  rounds: Round[];
  sessionId: number;
}

export function SessionDashboard({
  session,
  rounds,
  sessionId,
}: SessionDashboardProps) {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const createMutation = useCreateRoundMutation();
  const { myAmount, hasSettlementData } = useMySettlementAmount(session.id);

  const {
    deleteTarget,
    closeTarget,
    setCloseTarget,
    setDeleteTarget,
    handleReorder,
    handleToggleStatus,
    handleCloseConfirm,
    handleDelete,
    handleConfirmDelete,
    isStatusPending,
    isDeletePending,
  } = useRoundManagement(sessionId);

  const handleCreateRound = (data: {
    title: string;
    place: string;
    roundDate: string;
    storeId?: number;
  }) => {
    createMutation.mutate(
      {
        sessionId,
        request: {
          title: data.title,
          place: data.place || undefined,
          roundDate: data.roundDate || undefined,
          storeId: data.storeId,
        },
      },
      {
        onSuccess: () => setIsCreateModalOpen(false),
      }
    );
  };

  const isArchived = session.status === 'ARCHIVED';

  return (
    <>
      {/* 요약 카드 */}
      <Card className="mb-6 bg-primary text-primary-foreground border-0">
        <CardContent className="p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <p className="text-sm opacity-90 mb-1">총 지출</p>
              <p className="text-2xl font-bold">
                {formatAmount(session.totalAmount ?? 0)}원
              </p>
            </div>
            <div className="text-right">
              <p className="text-sm opacity-90 mb-1">{hasSettlementData ? '나의 청구액' : '나의 예상 청구액'}</p>
              <p className="text-2xl font-bold">
                {myAmount !== null ? `${formatAmount(myAmount)}원` : '-'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-4 text-sm opacity-90">
            <span>{session.memberCount ?? 0}명 참여</span>
            <span>{session.roundCount ?? 0}개 라운드</span>
          </div>
        </CardContent>
      </Card>

      {/* 라운드 타임라인 */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-foreground">
            라운드 타임라인
          </h2>
        </div>

        {rounds.length === 0 ? (
          <EmptyState
            icon={<ClockIcon className="w-8 h-8" />}
            title="아직 등록된 라운드가 없어요"
            description="첫 번째 라운드를 만들어 보세요"
            action={isArchived ? undefined : {
              label: '첫 라운드 만들기',
              onClick: () => setIsCreateModalOpen(true),
            }}
          />
        ) : (
          <>
            <RoundTimeline
              rounds={rounds}
              sessionId={String(sessionId)}
              onToggleStatus={handleToggleStatus}
              onDelete={handleDelete}
              isStatusPending={isStatusPending}
              onReorder={handleReorder}
              isSessionArchived={isArchived}
            />

            {/* 라운드 추가 버튼 */}
            {!isArchived && rounds.length < 10 && (
              <Button
                variant="outline"
                className="w-full mt-4 border-dashed bg-transparent"
                onClick={() => setIsCreateModalOpen(true)}
              >
                <PlusIcon className="w-4 h-4 mr-2" />새 라운드 추가
              </Button>
            )}
          </>
        )}
      </section>

      <RoundCreateModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSubmit={handleCreateRound}
        isPending={createMutation.isPending}
      />

      {/* 라운드 마감 확인 모달 */}
      <ConfirmModal
        isOpen={!!closeTarget}
        onClose={() => setCloseTarget(null)}
        onConfirm={handleCloseConfirm}
        title="정말 라운드를 마감할까요?"
        description="마감하면 메뉴·주문·그룹을 더 이상 수정할 수 없어요."
        confirmText="마감"
        isLoading={isStatusPending}
      />

      {/* 라운드 삭제 확인 모달 */}
      <DeleteConfirmModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        itemName={deleteTarget?.title}
        isLoading={isDeletePending}
      />
    </>
  );
}
