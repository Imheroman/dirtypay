'use client';

import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

interface RoundEditNameModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentTitle: string;
  onSubmit: (title: string) => void;
  isPending?: boolean;
}

export function RoundEditNameModal({
  isOpen,
  onClose,
  currentTitle,
  onSubmit,
  isPending,
}: RoundEditNameModalProps) {
  const [title, setTitle] = useState(currentTitle);

  useEffect(() => {
    if (isOpen) setTitle(currentTitle);
  }, [isOpen, currentTitle]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    onSubmit(title.trim());
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>라운드 이름 수정</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="edit-title">이름</Label>
            <Input
              id="edit-title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              autoFocus
            />
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button type="button" variant="outline" onClick={onClose} className="bg-transparent">
              취소
            </Button>
            <Button type="submit" disabled={!title.trim() || isPending}>
              {isPending ? '수정 중...' : '수정'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
