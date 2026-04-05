'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { ChevronLeftIcon, EditIcon, MapPinIcon, PhoneIcon } from '@/components/common/Icons';
import {
  useStoreQuery,
  useStoreMenusQuery,
  useToggleStoreMenuMutation,
  useDeleteStoreMenuMutation,
  useCreateStoreMenuMutation,
  useUpdateStoreMenuMutation,
  StoreMenuList,
  StoreMenuForm,
  StoreStatusBadge,
  StoreOrderList,
  StoreReviewList,
  type CreateStoreMenuRequest,
  type UpdateStoreMenuRequest,
  type StoreMenu,
} from '@/features/store';
import { getStoreTypeLabel, parseStoreDescription } from '@/features/store/utils';
import { LoadingSpinner } from '@/components/common';
import { DeleteConfirmModal } from '@/components/common/ConfirmModal';

export default function StoreDetailPage() {
  const params = useParams();
  const storeId = params?.storeId as string;

  const [isMenuFormOpen, setIsMenuFormOpen] = useState(false);
  const [selectedMenu, setSelectedMenu] = useState<StoreMenu | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ id: number; name: string } | null>(null);

  const { data: store, isLoading: isStoreLoading } = useStoreQuery(storeId);
  const { data: menus = [], isLoading: isMenusLoading } = useStoreMenusQuery(storeId);
  const toggleMenuMutation = useToggleStoreMenuMutation();
  const deleteMenuMutation = useDeleteStoreMenuMutation();
  const createMenuMutation = useCreateStoreMenuMutation(() => {
    setIsMenuFormOpen(false);
    setSelectedMenu(null);
  });
  const updateMenuMutation = useUpdateStoreMenuMutation(() => {
    setIsMenuFormOpen(false);
    setSelectedMenu(null);
  });

  const handleMenuEdit = (menu: StoreMenu) => {
    setSelectedMenu(menu);
    setIsMenuFormOpen(true);
  };

  const handleMenuDelete = (menu: StoreMenu) => {
    setDeleteTarget({ id: menu.id, name: menu.name });
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    deleteMenuMutation.mutate(
      { storeId, menuId: String(deleteTarget.id) },
      {
        onSuccess: () => setDeleteTarget(null),
      }
    );
  };

  const handleToggleAvailability = (menuId: number) => {
    toggleMenuMutation.mutate({ storeId, menuId: String(menuId) });
  };

  const handleMenuSubmit = (request: CreateStoreMenuRequest | UpdateStoreMenuRequest) => {
    if (selectedMenu) {
      updateMenuMutation.mutate({
        storeId,
        menuId: String(selectedMenu.id),
        request: request as UpdateStoreMenuRequest,
      });
    } else {
      createMenuMutation.mutate({
        storeId,
        request: request as CreateStoreMenuRequest,
      });
    }
  };

  if (isStoreLoading) {
    return <LoadingSpinner />;
  }

  if (!store) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
            <Link href="/stores" className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors">
              <ChevronLeftIcon className="w-5 h-5 text-foreground" />
            </Link>
            <h1 className="text-lg font-semibold text-foreground flex-1">가게 상세</h1>
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-lg">
          <Card className="p-6 text-center">
            <p className="text-muted-foreground">가게 정보를 불러오지 못했어요.</p>
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
          <Link href="/stores" className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors">
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1 truncate">{store.name}</h1>
          <Link href={`/stores/${storeId}/edit`}>
            <Button variant="ghost" size="icon">
              <EditIcon className="w-5 h-5" />
            </Button>
          </Link>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg space-y-6">
        {/* 가게 정보 카드 */}
        <Card>
          <CardHeader>
            <div className="flex items-start justify-between">
              <div>
                <CardTitle className="text-xl">{store.name}</CardTitle>
                <p className="text-sm text-muted-foreground mt-1">
                  {getStoreTypeLabel(store.storeType, store.description)}
                </p>
              </div>
              <StoreStatusBadge status={store.status} />
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            {/* 주소 */}
            <div className="flex gap-3">
              <MapPinIcon className="w-5 h-5 text-muted-foreground flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="text-xs text-muted-foreground">주소</p>
                <p className="text-sm text-foreground">{store.address}</p>
              </div>
            </div>

            {/* 전화번호 */}
            {store.phone && (
              <div className="flex gap-3">
                <PhoneIcon className="w-5 h-5 text-muted-foreground flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="text-xs text-muted-foreground">전화</p>
                  <p className="text-sm text-foreground">{store.phone}</p>
                </div>
              </div>
            )}

            {/* 소개 */}
            {(() => {
              const { cleanDescription } = parseStoreDescription(store.description);
              return cleanDescription ? (
                <div>
                  <p className="text-xs text-muted-foreground mb-1">소개</p>
                  <p className="text-sm text-foreground">{cleanDescription}</p>
                </div>
              ) : null;
            })()}

            {/* 사업자등록번호 */}
            {store.businessNumber && (
              <div>
                <p className="text-xs text-muted-foreground mb-1">사업자등록번호</p>
                <p className="text-sm text-foreground font-mono">{store.businessNumber}</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* 탭 섹션: 메뉴 / 주문 / 리뷰 */}
        <Tabs defaultValue="menu">
          <TabsList className="w-full">
            <TabsTrigger value="menu" className="flex-1">메뉴</TabsTrigger>
            <TabsTrigger value="orders" className="flex-1">주문</TabsTrigger>
            <TabsTrigger value="reviews" className="flex-1">리뷰</TabsTrigger>
          </TabsList>

          <TabsContent value="menu">
            <div className="mt-4 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold">메뉴 목록</h2>
                <Button size="sm" onClick={() => setIsMenuFormOpen(true)}>
                  메뉴 추가
                </Button>
              </div>
              <StoreMenuList
                menus={menus}
                isLoading={isMenusLoading}
                onToggleAvailability={handleToggleAvailability}
                onEdit={handleMenuEdit}
                onDelete={handleMenuDelete}
              />
            </div>
          </TabsContent>

          <TabsContent value="orders">
            <div className="mt-4">
              <StoreOrderList storeId={storeId} />
            </div>
          </TabsContent>

          <TabsContent value="reviews">
            <div className="mt-4">
              <StoreReviewList storeId={storeId} />
            </div>
          </TabsContent>
        </Tabs>
      </main>

      {/* 메뉴 폼 다이얼로그 */}
      {selectedMenu ? (
        <StoreMenuForm
          isOpen={isMenuFormOpen}
          onClose={() => {
            setIsMenuFormOpen(false);
            setSelectedMenu(null);
          }}
          onSubmit={handleMenuSubmit}
          menu={selectedMenu}
        />
      ) : (
        <StoreMenuForm
          isOpen={isMenuFormOpen}
          onClose={() => {
            setIsMenuFormOpen(false);
            setSelectedMenu(null);
          }}
          onSubmit={handleMenuSubmit}
        />
      )}

      {/* 메뉴 삭제 확인 다이얼로그 */}
      <DeleteConfirmModal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        itemName={deleteTarget?.name}
        isLoading={deleteMenuMutation.isPending}
      />
    </div>
  );
}
