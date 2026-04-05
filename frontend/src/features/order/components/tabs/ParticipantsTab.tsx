'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { UserIcon } from '@/components/common/Icons';
import { cn } from '@/lib/utils';
import type { RoundParticipant } from '@/features/round/types';

interface ParticipantsTabProps {
  participants: RoundParticipant[];
  currentOrgMemberId?: number;
}

export function ParticipantsTab({
  participants,
  currentOrgMemberId,
}: ParticipantsTabProps) {
  return (
    <div className="space-y-2">
      {participants.length === 0 && (
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-muted-foreground">
              아직 참여자가 없어요
            </p>
          </CardContent>
        </Card>
      )}
      {participants.map((participant) => {
        const isCurrentUser = currentOrgMemberId
          ? participant.orgMemberId === currentOrgMemberId
          : false;

        return (
          <Card
            key={participant.id}
            className={cn(isCurrentUser && 'border-primary bg-primary/5')}
          >
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div
                  className={cn(
                    'w-10 h-10 rounded-full flex items-center justify-center',
                    isCurrentUser
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-secondary text-secondary-foreground'
                  )}
                >
                  <UserIcon className="w-5 h-5" />
                </div>
                <div>
                  <p className="font-medium text-foreground">
                    {participant.nickname}
                    {isCurrentUser && (
                      <Badge variant="secondary" className="ml-2 text-xs">
                        나
                      </Badge>
                    )}
                  </p>
                  {participant.isExcluded && (
                    <p className="text-xs text-muted-foreground">정산 제외</p>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
