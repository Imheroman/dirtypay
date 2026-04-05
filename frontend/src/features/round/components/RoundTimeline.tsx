'use client';

import { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  TouchSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragStartEvent,
  type DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
  arrayMove,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { cn } from '@/lib/utils';
import { RoundCard } from './RoundCard';
import type { Round } from '../types';

interface RoundTimelineProps {
  rounds: Round[];
  sessionId: string;
  onToggleStatus: (round: Round) => void;
  onDelete: (round: Round) => void;
  isStatusPending?: boolean;
  onReorder: (rounds: { id: number; sortOrder: number }[]) => void;
  isSessionArchived?: boolean;
}

interface SortableRoundItemProps {
  round: Round;
  roundNumber: number;
  sessionId: string;
  isLast: boolean;
  onToggleStatus: (round: Round) => void;
  onDelete: (round: Round) => void;
  isStatusPending?: boolean;
  isSessionArchived?: boolean;
}

function SortableRoundItem({
  round,
  roundNumber,
  sessionId,
  isLast,
  onToggleStatus,
  onDelete,
  isStatusPending,
  isSessionArchived,
}: SortableRoundItemProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: round.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn('relative flex gap-4', !isLast && 'pb-6')}
      {...attributes}
    >
      {/* 타임라인 dot 노드 */}
      <div className="relative z-10 mt-3 shrink-0">
        <div
          className={cn(
            'w-3 h-3 rounded-full border-2',
            round.status === 'OPEN'
              ? 'bg-primary border-primary'
              : 'bg-muted border-muted-foreground/30'
          )}
        />
      </div>

      {/* 라운드 카드 */}
      <div className="flex-1 min-w-0">
        <RoundCard
          round={round}
          roundNumber={roundNumber}
          sessionId={sessionId}
          onToggleStatus={onToggleStatus}
          onDelete={onDelete}
          isStatusPending={isStatusPending}
          dragHandleProps={listeners}
          isDragging={isDragging}
          isSessionArchived={isSessionArchived}
        />
      </div>
    </div>
  );
}

export function RoundTimeline({
  rounds,
  sessionId,
  onToggleStatus,
  onDelete,
  isStatusPending,
  onReorder,
  isSessionArchived,
}: RoundTimelineProps) {
  const [activeRound, setActiveRound] = useState<Round | null>(null);

  const pointerSensor = useSensor(PointerSensor, {
    activationConstraint: { distance: 8 },
  });
  const touchSensor = useSensor(TouchSensor, {
    activationConstraint: { delay: 200, tolerance: 5 },
  });
  const sensors = useSensors(pointerSensor, touchSensor);

  const handleDragStart = (event: DragStartEvent) => {
    const round = rounds.find((r) => r.id === event.active.id);
    setActiveRound(round ?? null);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveRound(null);

    const { active, over } = event;
    if (!over || active.id === over.id) return;

    const oldIndex = rounds.findIndex((r) => r.id === active.id);
    const newIndex = rounds.findIndex((r) => r.id === over.id);
    if (oldIndex === -1 || newIndex === -1) return;

    const reordered = arrayMove(rounds, oldIndex, newIndex);
    const updates = reordered.map((r, i) => ({ id: r.id, sortOrder: i }));
    onReorder(updates);
  };

  const roundIds = rounds.map((r) => r.id);

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <SortableContext items={roundIds} strategy={verticalListSortingStrategy}>
        <div className="relative">
          {/* 세로 연결선 */}
          {rounds.length > 1 && (
            <div className="absolute left-[5px] top-3 bottom-9 w-0.5 bg-border" />
          )}

          <div className="space-y-0">
            {rounds.map((round, index) => (
              <SortableRoundItem
                key={round.id}
                round={round}
                roundNumber={index + 1}
                sessionId={sessionId}
                isLast={index === rounds.length - 1}
                onToggleStatus={onToggleStatus}
                onDelete={onDelete}
                isStatusPending={isStatusPending}
                isSessionArchived={isSessionArchived}
              />
            ))}
          </div>
        </div>
      </SortableContext>

      <DragOverlay>
        {activeRound && (
          <div className="opacity-90 shadow-lg">
            <RoundCard
              round={activeRound}
              roundNumber={
                rounds.findIndex((r) => r.id === activeRound.id) + 1
              }
              sessionId={sessionId}
              onToggleStatus={onToggleStatus}
              onDelete={onDelete}
              isStatusPending={isStatusPending}
              isSessionArchived={isSessionArchived}
            />
          </div>
        )}
      </DragOverlay>
    </DndContext>
  );
}
