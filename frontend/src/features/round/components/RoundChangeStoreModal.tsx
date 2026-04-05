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
import { StoreSearchPicker } from './StoreSearchPicker';
import type { PickerStore } from './StoreSearchPicker';

interface RoundChangeStoreModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentStoreId?: number;
  onSubmit: (storeId: number) => void;
  isPending?: boolean;
}

export function RoundChangeStoreModal({
  isOpen,
  onClose,
  onSubmit,
  isPending,
}: RoundChangeStoreModalProps) {
  const [selectedStore, setSelectedStore] = useState<PickerStore | null>(null);

  useEffect(() => {
    if (isOpen) setSelectedStore(null);
  }, [isOpen]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStore) return;
    onSubmit(selectedStore.id);
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>가게 변경</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <StoreSearchPicker
            selectedStore={selectedStore}
            onSelectStore={setSelectedStore}
            onClearStore={() => setSelectedStore(null)}
          />
          <DialogFooter className="gap-2 sm:gap-0">
            <Button type="button" variant="outline" onClick={onClose} className="bg-transparent">
              취소
            </Button>
            <Button type="submit" disabled={!selectedStore || isPending}>
              {isPending ? '변경 중...' : '변경'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
