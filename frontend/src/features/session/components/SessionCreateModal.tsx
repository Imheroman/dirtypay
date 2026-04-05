'use client';

import { useState, useRef } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useCreateSessionMutation } from '../hooks/useCreateSessionMutation';
import type { CreateSessionRequest } from '../types';

interface SessionCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export function SessionCreateModal({
  isOpen,
  onClose,
  onSuccess,
}: SessionCreateModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const titleInputRef = useRef<HTMLInputElement>(null);

  const createMutation = useCreateSessionMutation();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) return;

    const request: CreateSessionRequest = {
      title: title.trim(),
      description: description.trim() || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    };

    try {
      await createMutation.mutateAsync(request);
      handleClose();
      onSuccess?.();
      // TODO: story-7.1 (Toast 시스템)에서 Toast 피드백 추가
    } catch {
      // TODO: story-7.1 (Toast 시스템)에서 에러 피드백 추가
    }
  };

  const handleClose = () => {
    setTitle('');
    setDescription('');
    setStartDate('');
    setEndDate('');
    onClose();
  };

  // 모달이 열릴 때 제목 필드에 자동 포커스
  const handleOpenChange = (open: boolean) => {
    if (open) {
      // 다음 렌더링 사이클에서 포커스
      setTimeout(() => titleInputRef.current?.focus(), 0);
    } else {
      handleClose();
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>새 세션 만들기</DialogTitle>
          <DialogDescription>
            정산할 모임의 정보를 입력해 주세요
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">세션 이름</Label>
            <Input
              ref={titleInputRef}
              id="title"
              placeholder="예: 2024년 신년 회식"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              autoFocus
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">설명 (선택)</Label>
            <Input
              id="description"
              placeholder="세션에 대한 간단한 설명"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="startDate">시작일 (선택)</Label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="endDate">종료일 (선택)</Label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleClose}>
              취소
            </Button>
            <Button type="submit" disabled={!title.trim() || createMutation.isPending}>
              {createMutation.isPending ? '만드는 중...' : '만들기'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
