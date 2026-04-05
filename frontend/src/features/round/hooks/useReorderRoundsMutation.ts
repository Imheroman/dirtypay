'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/hooks/queries/keys';
import { roundApi } from '../api';
import type { Round } from '../types';

interface ReorderRoundsParams {
  sessionId: number;
  rounds: { id: number; sortOrder: number }[];
}

export function useReorderRoundsMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sessionId, rounds: reorderedRounds }: ReorderRoundsParams) => {
      const cachedRounds =
        queryClient.getQueryData<Round[]>(
          queryKeys.rounds.lists(String(sessionId))
        ) ?? [];

      return Promise.all(
        reorderedRounds.map((r) => {
          const cached = cachedRounds.find((cr) => cr.id === r.id);
          return roundApi.updateRound(r.id, {
            title: cached?.title ?? '',
            sortOrder: r.sortOrder,
          });
        })
      );
    },
    onMutate: async ({ sessionId, rounds: reorderedRounds }) => {
      const queryKey = queryKeys.rounds.lists(String(sessionId));
      await queryClient.cancelQueries({ queryKey });

      const previousRounds = queryClient.getQueryData<Round[]>(queryKey);

      queryClient.setQueryData<Round[]>(queryKey, (old) => {
        if (!old) return old;
        const orderMap = new Map(
          reorderedRounds.map((r) => [r.id, r.sortOrder])
        );
        return [...old]
          .map((round) => {
            const newOrder = orderMap.get(round.id);
            return newOrder !== undefined
              ? { ...round, sortOrder: newOrder }
              : round;
          })
          .sort((a, b) => a.sortOrder - b.sortOrder);
      });

      return { previousRounds, queryKey };
    },
    onError: (_err, _vars, context) => {
      if (context?.previousRounds) {
        queryClient.setQueryData(context.queryKey, context.previousRounds);
      }
    },
    onSettled: (_data, _err, { sessionId }) => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.rounds.lists(String(sessionId)),
      });
    },
  });
}
