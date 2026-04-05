'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { UsersIcon, CheckIcon } from '@/components/common/Icons';
import { cn } from '@/lib/utils';
import type { RoundGroup } from '../types';

interface GroupCreateDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  parentGroups?: RoundGroup[];
  defaultParentId?: number;
  onSubmit: (data: { name: string; parentGroupId?: number }) => void;
}

export function GroupCreateDialog({
  open,
  onOpenChange,
  parentGroups = [],
  defaultParentId,
  onSubmit,
}: GroupCreateDialogProps) {
  const [name, setName] = useState('');
  const [parentGroupId, setParentGroupId] = useState<number | undefined>(defaultParentId);

  const handleSubmit = () => {
    if (!name.trim()) return;

    onSubmit({
      name: name.trim(),
      parentGroupId,
    });

    setName('');
    setParentGroupId(undefined);
    onOpenChange(false);
  };

  const handleCancel = () => {
    setName('');
    setParentGroupId(undefined);
    onOpenChange(false);
  };

  // 부모 그룹 목록을 평탄화 (재귀적으로 모든 그룹 수집)
  const flattenGroups = (groups: RoundGroup[], depth = 0): Array<{ group: RoundGroup; depth: number }> => {
    const result: Array<{ group: RoundGroup; depth: number }> = [];
    for (const group of groups) {
      result.push({ group, depth });
      if (group.childGroups.length > 0) {
        result.push(...flattenGroups(group.childGroups, depth + 1));
      }
    }
    return result;
  };

  const flatParentGroups = flattenGroups(parentGroups);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <UsersIcon className="w-5 h-5 text-primary" />
            새 그룹 만들기
          </DialogTitle>
          <DialogDescription>
            같이 주문할 사람들을 모아 그룹을 만들어보세요
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div>
            <label className="text-sm font-medium text-foreground">그룹 이름</label>
            <Input
              placeholder="예: 맥주팀, 1팀"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1.5"
              autoFocus
            />
          </div>

          {parentGroups.length > 0 && (
            <div>
              <label className="text-sm font-medium text-foreground">
                상위 그룹 <span className="text-muted-foreground font-normal">(선택)</span>
              </label>
              <div className="mt-2 space-y-1 max-h-40 overflow-auto">
                <button
                  type="button"
                  onClick={() => setParentGroupId(undefined)}
                  className={cn(
                    'w-full flex items-center justify-between p-2 rounded-lg text-left text-sm transition-colors',
                    parentGroupId === undefined
                      ? 'bg-primary/10 border border-primary/30'
                      : 'bg-muted/50 hover:bg-muted'
                  )}
                >
                  <span className="text-foreground">없음 (최상위)</span>
                  {parentGroupId === undefined && (
                    <CheckIcon className="w-4 h-4 text-primary" />
                  )}
                </button>
                {flatParentGroups.map(({ group, depth }) => (
                  <button
                    key={group.groupId}
                    type="button"
                    onClick={() => setParentGroupId(group.groupId)}
                    className={cn(
                      'w-full flex items-center justify-between p-2 rounded-lg text-left text-sm transition-colors',
                      parentGroupId === group.groupId
                        ? 'bg-primary/10 border border-primary/30'
                        : 'bg-muted/50 hover:bg-muted'
                    )}
                    style={{ paddingLeft: `${(depth + 1) * 12}px` }}
                  >
                    <span className="text-foreground">
                      {depth > 0 && '└ '}{group.groupName}
                      <Badge variant="outline" className="ml-2 text-xs">
                        {group.members.length}명
                      </Badge>
                    </span>
                    {parentGroupId === group.groupId && (
                      <CheckIcon className="w-4 h-4 text-primary" />
                    )}
                  </button>
                ))}
              </div>
              <p className="text-xs text-muted-foreground mt-1.5">
                상위 그룹을 선택하면 해당 그룹의 하위 그룹으로 생성됩니다
              </p>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" className="bg-transparent" onClick={handleCancel}>
            취소
          </Button>
          <Button onClick={handleSubmit} disabled={!name.trim()}>
            만들기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
