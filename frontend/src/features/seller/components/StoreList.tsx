"use client";

import { useState } from "react";
import { StoreIcon } from "@/components/common/Icons";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { StoreCard } from "./StoreCard";
import { useDeleteStoreMutation } from "../hooks";
import type { Store } from "../types";

interface StoreListProps {
  stores: Store[] | undefined;
  isLoading: boolean;
  error: Error | null;
  onRetry: () => void;
  onCreateStore: () => void;
}

export function StoreList({
  stores,
  isLoading,
  error,
  onRetry,
  onCreateStore,
}: StoreListProps) {
  const [deleteTarget, setDeleteTarget] = useState<Store | null>(null);
  const deleteMutation = useDeleteStoreMutation();

  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-[180px] rounded-lg" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <ErrorMessage
        message="매장 목록을 불러오지 못했어요."
        onRetry={onRetry}
      />
    );
  }

  if (!stores?.length) {
    return (
      <EmptyState
        icon={<StoreIcon className="w-6 h-6 text-muted-foreground" />}
        title="등록된 매장이 없어요"
        description="첫 번째 매장을 등록해 보세요"
        action={{ label: "매장 등록", onClick: onCreateStore }}
      />
    );
  }

  return (
    <>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {stores.map((store) => (
          <StoreCard key={store.id} store={store} onDelete={setDeleteTarget} />
        ))}
      </div>

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>매장 삭제</DialogTitle>
            <DialogDescription>
              정말 &quot;{deleteTarget?.name}&quot;을(를) 삭제할까요? 삭제하면
              되돌릴 수 없어요.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteTarget(null)}
              className="bg-transparent"
            >
              취소
            </Button>
            <Button
              variant="destructive"
              onClick={handleDeleteConfirm}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? "삭제 중..." : "삭제"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
