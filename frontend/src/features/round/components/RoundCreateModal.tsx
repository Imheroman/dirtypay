'use client';

import { useState } from 'react';
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
import { StoreSearchPicker } from './StoreSearchPicker';
import type { PickerStore } from './StoreSearchPicker';

interface RoundCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: {
    title: string;
    place: string;
    roundDate: string;
    storeId: number;
  }) => void;
  isPending?: boolean;
}

export function RoundCreateModal({
  isOpen,
  onClose,
  onSubmit,
  isPending,
}: RoundCreateModalProps) {
  const [title, setTitle] = useState('');
  const [roundDate, setRoundDate] = useState('');
  const [selectedStore, setSelectedStore] = useState<PickerStore | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !selectedStore) return;

    onSubmit({
      title: title.trim(),
      place: selectedStore.name,
      roundDate,
      storeId: selectedStore.id,
    });

    handleClose();
  };

  const handleClose = () => {
    setTitle('');
    setRoundDate('');
    setSelectedStore(null);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>새 라운드 만들기</DialogTitle>
          <DialogDescription>
            새로운 식사나 모임 정보를 입력해주세요
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* 기본 정보 */}
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="title">라운드 이름 *</Label>
              <Input
                id="title"
                placeholder="예: 1일차 점심"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                autoFocus
              />
            </div>

            {/* 가게 선택 */}
            <div className="space-y-2">
              <Label>가게 *</Label>
              <StoreSearchPicker
                selectedStore={selectedStore}
                onSelectStore={setSelectedStore}
                onClearStore={() => setSelectedStore(null)}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="roundDate">날짜 (선택)</Label>
              <Input
                id="roundDate"
                type="datetime-local"
                value={roundDate}
                onChange={(e) => setRoundDate(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              className="bg-transparent"
            >
              취소
            </Button>
            <Button type="submit" disabled={!title.trim() || !selectedStore || isPending}>
              {isPending ? '만드는 중...' : '만들기'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
