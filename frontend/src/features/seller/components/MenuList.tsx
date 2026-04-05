"use client";

import { useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { PlusIcon, EditIcon, TrashIcon } from "@/components/common/Icons";
import { EmptyList } from "@/components/common/EmptyState";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { Skeleton } from "@/components/ui/skeleton";
import { formatAmount } from "@/lib/format";
import { MenuForm } from "./MenuForm";
import { useMenusQuery, useDeleteMenuMutation } from "../hooks";
import type { Menu } from "../types";

interface MenuListProps {
  storeId: number;
}

export function MenuList({ storeId }: MenuListProps) {
  const { data: menus, isLoading, error, refetch } = useMenusQuery({ storeId });
  const deleteMutation = useDeleteMenuMutation();
  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Menu | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Menu | null>(null);

  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(
      { menuId: deleteTarget.id, storeId },
      { onSuccess: () => setDeleteTarget(null) },
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-[72px] rounded-lg" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <ErrorMessage
        message="메뉴를 불러오지 못했어요."
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-foreground">메뉴</h3>
        <Button size="sm" onClick={() => setCreateOpen(true)}>
          <PlusIcon className="w-4 h-4" />
          메뉴 추가
        </Button>
      </div>

      {!menus?.length ? (
        <EmptyList message="등록된 메뉴가 없어요" />
      ) : (
        <div className="space-y-2">
          {menus.map((menu) => (
            <Card key={menu.id}>
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-foreground truncate">
                      {menu.name}
                    </span>
                    {!menu.isAvailable && (
                      <Badge variant="outline" className="text-xs">
                        품절
                      </Badge>
                    )}
                    {menu.category && (
                      <Badge variant="secondary" className="text-xs">
                        {menu.category}
                      </Badge>
                    )}
                  </div>
                  {menu.description && (
                    <p className="text-sm text-muted-foreground truncate mt-0.5">
                      {menu.description}
                    </p>
                  )}
                </div>
                <div className="flex items-center gap-3 shrink-0 ml-4">
                  <span className="font-semibold text-foreground">
                    {formatAmount(menu.price)}원
                  </span>
                  <div className="flex items-center gap-1">
                    <button
                      className="p-1 rounded hover:bg-accent transition-colors"
                      onClick={() => setEditTarget(menu)}
                    >
                      <EditIcon className="w-4 h-4 text-muted-foreground" />
                    </button>
                    <button
                      className="p-1 rounded hover:bg-accent transition-colors"
                      onClick={() => setDeleteTarget(menu)}
                    >
                      <TrashIcon className="w-4 h-4 text-muted-foreground" />
                    </button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <MenuForm
        storeId={storeId}
        open={createOpen}
        onOpenChange={setCreateOpen}
      />

      {editTarget && (
        <MenuForm
          storeId={storeId}
          menu={editTarget}
          open={!!editTarget}
          onOpenChange={(open) => !open && setEditTarget(null)}
        />
      )}

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>메뉴 삭제</DialogTitle>
            <DialogDescription>
              정말 &quot;{deleteTarget?.name}&quot;을(를) 삭제할까요?
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
    </div>
  );
}
