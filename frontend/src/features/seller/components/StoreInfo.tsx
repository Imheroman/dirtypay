"use client";

import { useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { MapPinIcon, EditIcon } from "@/components/common/Icons";
import { formatDate } from "@/lib/format";
import { useUpdateStoreMutation } from "../hooks";
import { StateChangeModal } from "./StateChangeModal";
import type { Store } from "../types";

const statusConfig: Record<
  string,
  { label: string; variant: "default" | "secondary" | "outline" }
> = {
  OPEN: { label: "영업 중", variant: "default" },
  TEMPORARILY_CLOSED: { label: "임시 휴업", variant: "secondary" },
  CLOSED: { label: "운영 종료", variant: "outline" },
};

interface StoreInfoProps {
  store: Store;
}

export function StoreInfo({ store }: StoreInfoProps) {
  const config = statusConfig[store.status] ?? statusConfig.OPEN;
  const updateMutation = useUpdateStoreMutation();
  const [editOpen, setEditOpen] = useState(false);
  const [stateModalOpen, setStateModalOpen] = useState(false);
  const [editName, setEditName] = useState("");
  const [editAddress, setEditAddress] = useState("");
  const [editDescription, setEditDescription] = useState("");

  const handleEditOpen = () => {
    setEditName(store.name);
    setEditAddress(store.address);
    setEditDescription(store.description ?? "");
    setEditOpen(true);
  };

  const handleEditSave = () => {
    updateMutation.mutate(
      {
        storeId: store.id,
        request: {
          name: editName.trim(),
          address: editAddress.trim(),
          description: editDescription.trim() || undefined,
        },
      },
      { onSuccess: () => setEditOpen(false) },
    );
  };

  return (
    <>
      <Card>
        <CardContent className="p-6">
          <div className="flex items-start justify-between mb-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <h2 className="text-xl font-bold text-foreground">
                  {store.name}
                </h2>
                <Badge variant={config.variant} className="text-xs">
                  {config.label}
                </Badge>
              </div>
              <div className="flex items-center gap-1 text-sm text-muted-foreground">
                <MapPinIcon className="w-4 h-4" />
                <span>{store.address}</span>
              </div>
            </div>
            <button
              className="p-1 rounded hover:bg-accent transition-colors"
              onClick={handleEditOpen}
            >
              <EditIcon className="w-4 h-4 text-muted-foreground" />
            </button>
          </div>

          {store.description && (
            <p className="text-sm text-muted-foreground mb-4">
              {store.description}
            </p>
          )}

          <div className="flex items-center gap-3 pt-4 border-t border-border/50">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setStateModalOpen(true)}
            >
              상태 변경
            </Button>
            <span className="text-xs text-muted-foreground">
              등록일 {formatDate(store.createdDate)}
            </span>
          </div>
        </CardContent>
      </Card>

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>매장 정보 수정</DialogTitle>
            <DialogDescription>매장 정보를 수정해 주세요</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="edit-name">매장 이름</Label>
              <Input
                id="edit-name"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                maxLength={50}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-address">주소</Label>
              <Input
                id="edit-address"
                value={editAddress}
                onChange={(e) => setEditAddress(e.target.value)}
                maxLength={200}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-description">설명</Label>
              <Input
                id="edit-description"
                value={editDescription}
                onChange={(e) => setEditDescription(e.target.value)}
                placeholder="매장 설명 (선택)"
                maxLength={500}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setEditOpen(false)}
              className="bg-transparent"
            >
              취소
            </Button>
            <Button
              onClick={handleEditSave}
              disabled={
                !editName.trim() ||
                !editAddress.trim() ||
                updateMutation.isPending
              }
            >
              {updateMutation.isPending ? "저장 중..." : "저장"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <StateChangeModal
        store={store}
        open={stateModalOpen}
        onOpenChange={setStateModalOpen}
      />
    </>
  );
}
