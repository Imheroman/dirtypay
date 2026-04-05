'use client';

import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { useCreateMemberMutation } from '../hooks/useCreateMemberMutation';
import { useCreateNodeMutation } from '../hooks/useCreateNodeMutation';
import { useUpdateMemberMutation } from '../hooks/useUpdateMemberMutation';
import { useUpdateNodeMutation } from '../hooks/useUpdateNodeMutation';
import { isSystemNode } from '../types';
import type { NodeTree, Member } from '../types';

export type NodeDialogMode =
  | { action: 'add'; type: 'member' | 'group'; parentNodeId: number }
  | { action: 'edit'; type: 'member'; target: Member }
  | { action: 'edit'; type: 'group'; target: NodeTree };

interface NodeDialogProps {
  isOpen: boolean;
  onClose: () => void;
  mode: NodeDialogMode | null;
  sessionId: number;
}

const nodeNameSchema = z.object({
  name: z
    .string()
    .min(1, '이름을 입력해 주세요')
    .max(50, '50자 이내로 입력해 주세요'),
});

type NodeNameFormData = z.infer<typeof nodeNameSchema>;

const UI_MAP = {
  'add-member': { title: '멤버 추가', placeholder: '이름 입력', button: '추가', pending: '추가 중...' },
  'add-group': { title: '하위 그룹 추가', placeholder: '그룹명 입력', button: '추가', pending: '추가 중...' },
  'edit-member': { title: '멤버 이름 수정', placeholder: '이름 입력', button: '변경', pending: '변경 중...' },
  'edit-group': { title: '그룹 이름 수정', placeholder: '그룹명 입력', button: '변경', pending: '변경 중...' },
} as const;

function getUIKey(mode: NodeDialogMode): keyof typeof UI_MAP {
  return `${mode.action}-${mode.type}` as keyof typeof UI_MAP;
}

export function NodeDialog({ isOpen, onClose, mode, sessionId }: NodeDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    setFocus,
    formState: { errors, isValid },
  } = useForm<NodeNameFormData>({
    resolver: zodResolver(nodeNameSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  });

  const createMember = useCreateMemberMutation();
  const createNode = useCreateNodeMutation();
  const updateMember = useUpdateMemberMutation();
  const updateNode = useUpdateNodeMutation();

  const isPending =
    createMember.isPending || createNode.isPending ||
    updateMember.isPending || updateNode.isPending;

  useEffect(() => {
    if (!isOpen || !mode) return;
    if (mode.action === 'edit') {
      const defaultName = mode.type === 'member' ? mode.target.nickname : mode.target.name;
      reset({ name: defaultName });
    } else {
      reset({ name: '' });
    }
    setTimeout(() => setFocus('name'), 0);
  }, [isOpen, mode, reset, setFocus]);

  if (!mode) return null;

  const ui = UI_MAP[getUIKey(mode)];

  const onSubmit = (data: NodeNameFormData) => {
    const trimmed = data.name.trim();
    if (!trimmed) return;

    const onSuccess = () => onClose();

    if (mode.action === 'add' && mode.type === 'member') {
      createMember.mutate(
        { sessionId, request: { nickname: trimmed } },
        { onSuccess },
      );
    } else if (mode.action === 'add' && mode.type === 'group') {
      createNode.mutate(
        { sessionId, request: { parentNodeId: mode.parentNodeId, name: trimmed } },
        { onSuccess },
      );
    } else if (mode.action === 'edit' && mode.type === 'member') {
      updateMember.mutate(
        { id: mode.target.id, sessionId, request: { nickname: trimmed } },
        { onSuccess },
      );
    } else if (mode.action === 'edit' && mode.type === 'group') {
      if (isSystemNode(mode.target)) return;
      updateNode.mutate(
        { id: mode.target.id, sessionId, request: { name: trimmed } },
        { onSuccess },
      );
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{ui.title}</DialogTitle>
          <DialogDescription>
            {mode.action === 'add' ? '새로운 이름을 입력하세요' : '새로운 이름을 입력해주세요'}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="space-y-2">
            <Input
              placeholder={ui.placeholder}
              aria-invalid={!!errors.name}
              aria-describedby={errors.name ? 'name-error' : undefined}
              {...register('name')}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !isPending) {
                  e.preventDefault();
                  handleSubmit(onSubmit)();
                }
              }}
            />
            {errors.name && (
              <p id="name-error" role="alert" className="text-sm text-destructive">
                {errors.name.message}
              </p>
            )}
          </div>
          <DialogFooter className="mt-4">
            <Button type="button" variant="outline" onClick={onClose} disabled={isPending} className="bg-transparent">
              취소
            </Button>
            <Button type="submit" disabled={!isValid || isPending}>
              {isPending ? ui.pending : ui.button}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
