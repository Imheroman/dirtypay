"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useChangeStoreStateMutation } from "../hooks";
import type { Store, StoreStatus } from "../types";

const statusOptions: {
  value: StoreStatus;
  label: string;
  description: string;
}[] = [
  { value: "OPEN", label: "영업 중", description: "주문을 받을 수 있어요" },
  {
    value: "TEMPORARILY_CLOSED",
    label: "임시 휴업",
    description: "일시적으로 주문을 받지 않아요",
  },
  {
    value: "CLOSED",
    label: "운영 종료",
    description: "더 이상 운영하지 않아요",
  },
];

interface StateChangeModalProps {
  store: Store;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function StateChangeModal({
  store,
  open,
  onOpenChange,
}: StateChangeModalProps) {
  const [selected, setSelected] = useState<StoreStatus>(store.status);
  const mutation = useChangeStoreStateMutation();

  const handleConfirm = () => {
    if (selected === store.status) {
      onOpenChange(false);
      return;
    }
    mutation.mutate(
      { storeId: store.id, request: { status: selected } },
      { onSuccess: () => onOpenChange(false) },
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>매장 상태 변경</DialogTitle>
          <DialogDescription>변경할 상태를 선택해 주세요</DialogDescription>
        </DialogHeader>
        <div className="space-y-2">
          {statusOptions.map((option) => (
            <button
              key={option.value}
              className={cn(
                "w-full flex flex-col items-start gap-1 p-3 rounded-lg border transition-colors text-left",
                selected === option.value
                  ? "border-primary bg-primary/5"
                  : "border-border hover:bg-accent",
              )}
              onClick={() => setSelected(option.value)}
            >
              <span className="font-medium text-sm text-foreground">
                {option.label}
              </span>
              <span className="text-xs text-muted-foreground">
                {option.description}
              </span>
            </button>
          ))}
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            className="bg-transparent"
          >
            취소
          </Button>
          <Button onClick={handleConfirm} disabled={mutation.isPending}>
            {mutation.isPending ? "변경 중..." : "변경"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
