'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ChevronLeftIcon, PlusIcon } from '@/components/common/Icons';
import { useStoresQuery, useDeleteStoreMutation, useUpdateStoreMutation, StoreCard, StoreForm, type Store, type UpdateStoreRequest } from '@/features/store';
import { EmptyState, LoadingSpinner } from '@/components/common';
import { DeleteConfirmModal } from '@/components/common/ConfirmModal';

export default function StoresPage() {
  const [selectedStore, setSelectedStore] = useState<Store | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<{ id: number; name: string } | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 20;

  const { data, isLoading, error } = useStoresQuery({
    page: currentPage,
    size: pageSize,
    scope: 'my',
  });
  const deleteStoreMutation = useDeleteStoreMutation();
  const updateStoreMutation = useUpdateStoreMutation();

  const stores = data?.content || [];
  const totalPages = data?.totalPages || 0;

  const handleEdit = (store: typeof selectedStore) => {
    setSelectedStore(store);
    setIsFormOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    deleteStoreMutation.mutate(
      { storeId: deleteTarget.id },
      {
        onSuccess: () => setDeleteTarget(null),
      }
    );
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
            <Link href="/" className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors">
              <ChevronLeftIcon className="w-5 h-5 text-foreground" />
            </Link>
            <h1 className="text-lg font-semibold text-foreground flex-1">가게 관리</h1>
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-lg">
          <Card className="p-6 text-center">
            <p className="text-muted-foreground">가게 목록을 불러오지 못했어요.</p>
          </Card>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link href="/" className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors">
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">가게 관리</h1>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        {/* 등록 버튼 */}
        <Link href="/stores/new" className="block mb-6">
          <Button className="w-full" size="lg">
            <PlusIcon className="w-5 h-5 mr-2" />
            새 가게 등록
          </Button>
        </Link>

        {/* 가게 목록 */}
        {stores.length === 0 ? (
          <EmptyState
            title="아직 등록된 가게가 없어요"
            description="새로운 가게를 등록해서 시작해보세요."
          />
        ) : (
          <div className="space-y-3 mb-8">
            {stores.map((store) => (
              <StoreCard
                key={store.id}
                store={store}
                onEdit={() => handleEdit(store)}
                onDelete={() => setDeleteTarget({ id: store.id, name: store.name })}
              />
            ))}
          </div>
        )}

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className="flex gap-2 justify-center py-4">
            <Button
              variant="outline"
              onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
              disabled={currentPage === 0}
            >
              이전
            </Button>
            <div className="flex items-center px-3">
              <span className="text-sm text-muted-foreground">
                {currentPage + 1} / {totalPages}
              </span>
            </div>
            <Button
              variant="outline"
              onClick={() => setCurrentPage(Math.min(totalPages - 1, currentPage + 1))}
              disabled={currentPage === totalPages - 1}
            >
              다음
            </Button>
          </div>
        )}
      </main>

      {/* 수정 폼 다이얼로그 */}
      {selectedStore && (
        <StoreForm
          isOpen={isFormOpen}
          onClose={() => {
            setIsFormOpen(false);
            setSelectedStore(null);
          }}
          onSubmit={(request) => {
            if (!selectedStore) return;
            updateStoreMutation.mutate(
              { storeId: selectedStore.id, request: request as UpdateStoreRequest },
              {
                onSuccess: () => {
                  setIsFormOpen(false);
                  setSelectedStore(null);
                },
              }
            );
          }}
          isLoading={updateStoreMutation.isPending}
          store={selectedStore}
        />
      )}

      {/* 삭제 확인 다이얼로그 */}
      <DeleteConfirmModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        itemName={deleteTarget?.name}
        isLoading={deleteStoreMutation.isPending}
      />
    </div>
  );
}
