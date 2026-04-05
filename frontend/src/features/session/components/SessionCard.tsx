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
  UsersIcon,
  CalendarIcon,
  WalletIcon,
  MoreVerticalIcon,
  ArchiveIcon,
  TrashIcon,
} from '@/components/common/Icons';
import { formatAmount } from '@/lib/format';
import type { Session } from '../types';

interface SessionCardProps {
  session: Session;
  onArchive?: (session: Session) => void;
  onDelete?: (session: Session) => void;
}

export function SessionCard({ session, onArchive, onDelete }: SessionCardProps) {
  const router = useRouter();
  const statusLabel = session.status === 'ACTIVE' ? '진행중' : '완료';
  const statusVariant = session.status === 'ACTIVE' ? 'default' : 'secondary';

  const handleCardClick = () => {
    router.push(`/sessions/${session.id}`);
  };

  const hasMenu = onArchive || onDelete;

  return (
    <Card
      className="hover:shadow-md hover:border-primary/30 hover:scale-[1.01] hover:bg-accent/50 transition-all duration-200 cursor-pointer border-border/60 h-full"
      onClick={handleCardClick}
    >
      <CardContent className="p-5">
        <div className="flex items-start justify-between mb-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <h4 className="font-semibold text-foreground truncate">{session.title}</h4>
              <Badge variant={statusVariant} className="text-xs shrink-0">
                {statusLabel}
              </Badge>
            </div>
            {session.description && (
              <p className="text-sm text-muted-foreground line-clamp-2">
                {session.description}
              </p>
            )}
          </div>

          {hasMenu && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  className="p-1 rounded-md hover:bg-accent transition-colors shrink-0 ml-2"
                  onClick={(e) => e.stopPropagation()}
                  aria-label="세션 메뉴"
                >
                  <MoreVerticalIcon className="w-5 h-5 text-muted-foreground" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" onClick={(e) => e.stopPropagation()}>
                {onArchive && session.status === 'ACTIVE' && (
                  <>
                    <DropdownMenuItem onClick={() => onArchive(session)}>
                      <ArchiveIcon className="w-4 h-4" />
                      마무리
                    </DropdownMenuItem>
                    {onDelete && <DropdownMenuSeparator />}
                  </>
                )}
                {onDelete && (
                  <DropdownMenuItem
                    className="text-destructive focus:text-destructive"
                    onClick={() => onDelete(session)}
                  >
                    <TrashIcon className="w-4 h-4" />
                    삭제
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>

        <div className="flex items-center gap-4 text-sm text-muted-foreground mt-4 pt-4 border-t border-border/50">
          <div className="flex items-center gap-1">
            <UsersIcon className="w-4 h-4" />
            <span>{session.memberCount ?? 0}명</span>
          </div>
          <div className="flex items-center gap-1">
            <CalendarIcon className="w-4 h-4" />
            <span>{session.roundCount ?? 0}라운드</span>
          </div>
        </div>
        <div className="flex items-center gap-1 mt-3">
          <WalletIcon className="w-4 h-4 text-primary" />
          <span className="font-semibold text-foreground">
            {formatAmount(session.totalAmount ?? 0)}원
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
