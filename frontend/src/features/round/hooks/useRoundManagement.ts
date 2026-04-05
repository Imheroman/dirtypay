'use client';

import { useState } from 'react';
import { useDeleteRoundMutation } from './useDeleteRoundMutation';
import { useUpdateRoundStatusMutation } from './useUpdateRoundStatusMutation';
import { useReorderRoundsMutation } from './useReorderRoundsMutation';
import type { Round } from '../types';

export function useRoundManagement(sessionId: number) {
  const [deleteTarget, setDeleteTarget] = useState<Round | null>(null);
  const [closeTarget, setCloseTarget] = useState<Round | null>(null);

  const deleteMutation = useDeleteRoundMutation();
  const statusMutation = useUpdateRoundStatusMutation();
  const reorderMutation = useReorderRoundsMutation();

  const handleReorder = (reorderedRounds: { id: number; sortOrder: number }[]) => {
    reorderMutation.mutate({
      sessionId,
      rounds: reorderedRounds,
    });
  };

  const handleToggleStatus = (round: Round) => {
    if (round.status === 'OPEN') {
      setCloseTarget(round);
      return;
    }
    statusMutation.mutate({
      roundId: round.id,
      sessionId,
      status: 'OPEN',
    });
  };

  const handleCloseConfirm = () => {
    if (!closeTarget) return;
    statusMutation.mutate(
      {
        roundId: closeTarget.id,
        sessionId,
        status: 'CLOSED',
      },
      {
        onSuccess: () => setCloseTarget(null),
      }
    );
  };

  const handleDelete = (round: Round) => {
    setDeleteTarget(round);
  };

  const handleConfirmDelete = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(
      {
        roundId: deleteTarget.id,
        sessionId,
      },
      {
        onSuccess: () => setDeleteTarget(null),
      }
    );
  };

  return {
    deleteTarget,
    setDeleteTarget,
    closeTarget,
    setCloseTarget,
    handleReorder,
    handleToggleStatus,
    handleCloseConfirm,
    handleDelete,
    handleConfirmDelete,
    isStatusPending: statusMutation.isPending,
    isDeletePending: deleteMutation.isPending,
  };
}
