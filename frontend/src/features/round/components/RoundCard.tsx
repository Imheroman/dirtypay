'use client';

import { useRouter } from 'next/navigation';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  MapPinIcon,
  ClockIcon,
  MoreVerticalIcon,
  UtensilsIcon,
  LockIcon,
  UnlockIcon,
  TrashIcon,
  GripVerticalIcon,
} from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import { cn } from '@/lib/utils';
import type { Round } from '../types';

interface RoundCardProps {
  round: Round;
  roundNumber: number;
  sessionId: string;
  onToggleStatus: (round: Round) => void;
  onDelete: (round: Round) => void;
  isStatusPending?: boolean;
  dragHandleProps?: React.HTMLAttributes<HTMLButtonElement>;
  isDragging?: boolean;
  isSessionArchived?: boolean;
}

export function RoundCard({
  round,
  roundNumber,
  sessionId,
  onToggleStatus,
  onDelete,
  isStatusPending,
  dragHandleProps,
  isDragging,
  isSessionArchived,
}: RoundCardProps) {
  const router = useRouter();

  const handleCardClick = () => {
    router.push(`/sessions/${sessionId}/rounds/${round.id}`);
  };

  return (
    <Card
      className={cn(
        'hover:shadow-md hover:border-primary/30 hover:scale-[1.01] transition-all duration-200 cursor-pointer',
        isDragging && 'opacity-50'
      )}
      onClick={handleCardClick}
    >
      <CardContent className="p-4">
        <div className="flex items-start gap-3">
          {/* 드래그 핸들 */}
          {dragHandleProps && (
            <button
              className="p-1 -ml-1 self-center cursor-grab active:cursor-grabbing text-muted-foreground/50 hover:text-muted-foreground transition-colors touch-none"
              onClick={(e) => e.stopPropagation()}
              aria-label="드래그하여 순서 변경"
              {...dragHandleProps}
            >
              <GripVerticalIcon className="w-5 h-5" />
            </button>
          )}

          {/* 라운드 번호 */}
          <div
            className={cn(
              'w-12 h-12 rounded-xl flex flex-col items-center justify-center shrink-0',
              round.status === 'OPEN'
                ? 'bg-primary text-primary-foreground'
                : 'bg-muted text-muted-foreground'
            )}
          >
            <span className="text-xs opacity-80">ROUND</span>
            <span className="text-lg font-bold -mt-0.5">{roundNumber}</span>
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <h3 className="font-semibold text-foreground truncate">
                {round.title}
              </h3>
              <Badge
                variant={round.status === 'OPEN' ? 'default' : 'secondary'}
                className="text-xs shrink-0"
              >
                {round.status === 'OPEN' ? '진행중' : '완료'}
              </Badge>
            </div>

            <div className="flex items-center gap-3 text-sm text-muted-foreground mb-2">
              {round.place && (
                <span className="flex items-center gap-1">
                  <MapPinIcon className="w-3.5 h-3.5" />
                  {round.place}
                </span>
              )}
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3 text-sm text-muted-foreground">
                <span className="flex items-center gap-1">
                  <ClockIcon className="w-3.5 h-3.5" />
                  {round.participantCount ?? 0}명 참여
                </span>
              </div>
              <p className="text-base font-bold text-foreground">
                {formatAmount(round.totalAmount ?? 0)}원
              </p>
            </div>
          </div>

          {/* ⋮ 드롭다운 메뉴 — ARCHIVED 세션이면 숨김 */}
          {!isSessionArchived && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  className="p-1 rounded-md hover:bg-accent transition-colors shrink-0 self-center"
                  onClick={(e) => e.stopPropagation()}
                  aria-label="라운드 메뉴"
                >
                  <MoreVerticalIcon className="w-5 h-5 text-muted-foreground" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" onClick={(e) => e.stopPropagation()}>
                <DropdownMenuItem
                  onClick={() => onToggleStatus(round)}
                  disabled={isStatusPending}
                >
                  {round.status === 'OPEN' ? (
                    <>
                      <LockIcon className="w-4 h-4" />
                      마감
                    </>
                  ) : (
                    <>
                      <UnlockIcon className="w-4 h-4" />
                      재개
                    </>
                  )}
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  className="text-destructive focus:text-destructive"
                  onClick={() => onDelete(round)}
                >
                  <TrashIcon className="w-4 h-4" />
                  삭제
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
