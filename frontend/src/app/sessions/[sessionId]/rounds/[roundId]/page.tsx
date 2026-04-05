'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import { use } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  ChevronLeftIcon,
  LockIcon,
  UnlockIcon,
  MoreVerticalIcon,
  TrashIcon,
  EditIcon,
  StoreIcon,
} from '@/components/common/Icons';
import { ErrorMessage } from '@/components/common/ErrorMessage';
import { ConfirmModal, DeleteConfirmModal } from '@/components/common/ConfirmModal';
import { formatAmount } from '@/lib/format';
import { toast } from 'sonner';
import { useRoundQuery, useUpdateRoundMutation, useUpdateRoundStatusMutation, useDeleteRoundMutation, useRoundParticipantsQuery, RoundEditNameModal, RoundChangeStoreModal } from '@/features/round';
import { useSessionQuery } from '@/features/session/hooks/useSessionQuery';
import { useAuthContext } from '@/components/providers/auth-provider';
import { useMembersQuery } from '@/features/organization';
import {
  ParticipantsTab,
  OrdersTab,
  GroupsTab,
  MenuTab,
  MyOrderTab,
  useGroupedOrdersQuery,
  useGroupedMenusQuery,
  useRoundGroupsQuery,
  useOrdersQuery,
  useCreateOrderMutation,
  useDeleteOrderMutation,
  findMyGroup,
  findCurrentOrgMemberId,
  getAllGroupMemberIds,
} from '@/features/order';
import type { CartItem } from '@/features/order';

interface PageProps {
  params: Promise<{ sessionId: string; roundId: string }>;
}

export default function RoundDetailPage({ params }: PageProps) {
  const { sessionId, roundId } = use(params);
  const router = useRouter();
  const [activeTab, setActiveTab] = useState('groups');
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [closeConfirmOpen, setCloseConfirmOpen] = useState(false);
  const [isEditNameOpen, setIsEditNameOpen] = useState(false);
  const [isChangeStoreOpen, setIsChangeStoreOpen] = useState(false);

  // 세션 조회 (readOnly 판별용)
  const { data: session } = useSessionQuery(Number(sessionId));

  // API 연동: 라운드 상세 조회
  const { data: round, isLoading, error, refetch } = useRoundQuery(roundId);

  // 라운드 상태 변경 / 삭제 / 수정 mutation
  const statusMutation = useUpdateRoundStatusMutation();
  const deleteMutation = useDeleteRoundMutation();
  const updateRoundMutation = useUpdateRoundMutation();

  // 주문/메뉴/그룹/참여자 데이터 조회
  const { data: ordersData } = useGroupedOrdersQuery(roundId);
  const { data: roundGroups } = useRoundGroupsQuery(roundId);
  const { data: participants = [] } = useRoundParticipantsQuery(Number(roundId));
  const { data: allOrders = [] } = useOrdersQuery(roundId);
  const { data: menusData } = useGroupedMenusQuery(round?.storeId);

  // Mutation hooks
  const createOrderMutation = useCreateOrderMutation();
  const deleteOrderMutation = useDeleteOrderMutation();

  // 마감/재개 버튼 핸들러
  const handleToggleStatus = () => {
    if (!round) return;
    if (round.status === 'OPEN') {
      setCloseConfirmOpen(true);
      return;
    }
    statusMutation.mutate({
      roundId: Number(roundId),
      sessionId: Number(sessionId),
      status: 'OPEN',
    });
  };

  const handleCloseConfirm = () => {
    statusMutation.mutate(
      {
        roundId: Number(roundId),
        sessionId: Number(sessionId),
        status: 'CLOSED',
      },
      {
        onSuccess: () => setCloseConfirmOpen(false),
      }
    );
  };

  // 라운드 삭제 핸들러
  const handleConfirmDelete = () => {
    deleteMutation.mutate(
      {
        roundId: Number(roundId),
        sessionId: Number(sessionId),
      },
      {
        onSuccess: () => {
          setIsDeleteModalOpen(false);
          router.push(`/sessions/${sessionId}`);
        },
      }
    );
  };

  // 라운드 이름 수정
  const handleEditName = (title: string) => {
    updateRoundMutation.mutate(
      {
        roundId: Number(roundId),
        sessionId: Number(sessionId),
        request: { title, place: round?.place },
      },
      { onSuccess: () => setIsEditNameOpen(false) }
    );
  };

  // 가게 변경
  const handleChangeStore = (storeId: number) => {
    updateRoundMutation.mutate(
      {
        roundId: Number(roundId),
        sessionId: Number(sessionId),
        request: { title: round?.title ?? '', storeId },
      },
      { onSuccess: () => setIsChangeStoreOpen(false) }
    );
  };

  // 현재 유저의 orgMemberId 찾기 (조직 멤버 매칭 우선, 그룹 폴백)
  const { user } = useAuthContext();
  const { data: members } = useMembersQuery(Number(sessionId));

  const currentMember = useMemo(() => {
    if (user && members) {
      return members.find(m => m.userId === user.id) ?? null;
    }
    return null;
  }, [user, members]);

  const currentOrgMemberId = useMemo(() => {
    if (currentMember) return currentMember.id;
    // 폴백: 그룹 기반
    return findCurrentOrgMemberId(roundGroups ?? []);
  }, [currentMember, roundGroups]);

  // 장바구니에서 모든 메뉴 일괄 주문
  const handlePlaceOrder = async (cartItems: CartItem[]) => {
    if (cartItems.length === 0) return;

    if (!currentOrgMemberId || !currentMember) {
      toast.error('그룹에 먼저 참여해 주세요.', {
        description: '그룹 탭에서 참여할 수 있어요.',
      });
      return;
    }

    // 그룹 참여 여부 확인
    const myGroup = findMyGroup(roundGroups ?? [], currentOrgMemberId);
    if (!myGroup) {
      toast.error('그룹에 먼저 참여해 주세요.', {
        description: '그룹 탭에서 참여할 수 있어요.',
      });
      return;
    }

    // 모든 메뉴에 대해 동시 API 호출 (mutateAsync 사용)
    // MenuTab은 항상 라운드 메뉴 API를 사용하므로 menu.id는 StoreMenu ID
    const requests = cartItems.map(({ menu, quantity }) =>
      createOrderMutation.mutateAsync({
        roundId: Number(roundId),
        request: {
          menuId: menu.id,
          quantity,
          memberIds: [currentOrgMemberId],
          groupId: myGroup.groupId,
        },
      })
    );

    try {
      await Promise.allSettled(requests);
      setActiveTab('orders');
    } catch {
      // 에러는 이미 toast로 처리됨
    }
  };

  // 주문 삭제
  const handleDeleteOrder = (orderId: number) => {
    deleteOrderMutation.mutate({
      orderId,
      roundId: Number(roundId),
    });
  };

  // 그룹에 참여하지 않은 라운드 참여자 (미배정)
  const groupMemberIds = useMemo(
    () => getAllGroupMemberIds(roundGroups ?? []),
    [roundGroups]
  );
  const unassignedParticipants = useMemo(
    () => participants.filter((p) => !groupMemberIds.has(p.orgMemberId)),
    [participants, groupMemberIds]
  );

  // 현재 유저의 주문만 필터링
  const myOrders = allOrders.filter((order) =>
    order.details.some((d) => d.orgMemberId === currentOrgMemberId)
  );

  // readOnly 판별: ARCHIVED 세션 또는 CLOSED 라운드
  const isSessionArchived = session?.status === 'ARCHIVED';
  const isReadOnly = isSessionArchived || round?.status === 'CLOSED';

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
            <Skeleton className="h-5 w-5 rounded" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-5 w-32" />
              <Skeleton className="h-4 w-24" />
            </div>
            <Skeleton className="h-8 w-16 rounded-md" />
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-lg">
          <Skeleton className="h-28 w-full rounded-xl mb-6" />
          <Skeleton className="h-10 w-full rounded-md mb-6" />
          <div className="space-y-3">
            <Skeleton className="h-20 w-full rounded-xl" />
            <Skeleton className="h-20 w-full rounded-xl" />
            <Skeleton className="h-20 w-full rounded-xl" />
          </div>
        </main>
      </div>
    );
  }

  // 에러 상태
  if (error || !round) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
            <Link
              href={`/sessions/${sessionId}`}
              className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
              aria-label="뒤로 가기"
            >
              <ChevronLeftIcon className="w-5 h-5 text-foreground" />
            </Link>
            <h1 className="text-lg font-semibold text-foreground">라운드</h1>
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-lg">
          <ErrorMessage
            message="라운드 정보를 불러오지 못했어요."
            onRetry={() => refetch()}
          />
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link
            href={`/sessions/${sessionId}`}
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-semibold text-foreground truncate">
                {round.title}
              </h1>
              <Badge
                variant={round.status === 'OPEN' ? 'default' : 'secondary'}
                className="shrink-0"
              >
                {round.status === 'OPEN' ? '진행중' : '완료'}
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground truncate">
              {round.place || ''}
            </p>
          </div>
          {/* ARCHIVED 세션이면 메뉴 숨김, CLOSED 라운드(ACTIVE 세션)면 재개+삭제만 표시 */}
          {!isSessionArchived && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  className="p-1.5 rounded-md hover:bg-accent transition-colors shrink-0"
                  aria-label="라운드 메뉴"
                >
                  <MoreVerticalIcon className="w-5 h-5 text-foreground" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem
                  onClick={handleToggleStatus}
                  disabled={statusMutation.isPending}
                >
                  {round.status === 'OPEN' ? (
                    <>
                      <LockIcon className="w-4 h-4" />
                      마감
                    </>
                  ) : (
                    <>
                      <UnlockIcon className="w-4 h-4" />
                      재개
                    </>
                  )}
                </DropdownMenuItem>
                {allOrders.length === 0 && round.status === 'OPEN' && (
                  <>
                    <DropdownMenuItem onClick={() => setIsEditNameOpen(true)}>
                      <EditIcon className="w-4 h-4" />
                      이름 수정
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setIsChangeStoreOpen(true)}>
                      <StoreIcon className="w-4 h-4" />
                      가게 변경
                    </DropdownMenuItem>
                  </>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  className="text-destructive focus:text-destructive"
                  onClick={() => setIsDeleteModalOpen(true)}
                >
                  <TrashIcon className="w-4 h-4" />
                  삭제
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        {/* 총액 카드 */}
        <Card className="mb-6 bg-primary/5 border-primary/20">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">이번 라운드 총액</p>
                <p className="text-3xl font-bold text-foreground">
                  {formatAmount(round.totalAmount ?? 0)}원
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm text-muted-foreground">참여자</p>
                <p className="text-2xl font-semibold text-foreground">
                  {round.participantCount ?? 0}명
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 탭 네비게이션 */}
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="groups">그룹</TabsTrigger>
            <TabsTrigger value="orders">주문 내역</TabsTrigger>
            <TabsTrigger value="participants">참여자</TabsTrigger>
            <TabsTrigger value="menu">메뉴판</TabsTrigger>
            <TabsTrigger value="myorder">내 주문</TabsTrigger>
          </TabsList>

          {/* 그룹 탭 */}
          <TabsContent value="groups" className="mt-6">
            <GroupsTab
              roundId={roundId}
              groups={roundGroups ?? []}
              availableMenus={menusData?.all ?? []}
              sessionId={Number(sessionId)}
              currentMemberId={currentOrgMemberId ?? 0}
              isReadOnly={isReadOnly}
              unassignedParticipants={unassignedParticipants}
            />
          </TabsContent>

          {/* 주문 내역 탭 */}
          <TabsContent value="orders" className="mt-6">
            <OrdersTab
              orderGroups={ordersData?.grouped ?? []}
              allOrders={allOrders}
              roundGroups={roundGroups ?? []}
              currentMemberId={currentOrgMemberId}
            />
          </TabsContent>

          {/* 참여자 탭 */}
          <TabsContent value="participants" className="mt-6">
            <ParticipantsTab participants={participants} />
          </TabsContent>

          {/* 메뉴판 탭 */}
          <TabsContent value="menu" className="mt-6 pb-80">
            <MenuTab
              storeId={round?.storeId}
              isSettled={isReadOnly}
              isReadOnly={isReadOnly}
              onAddToCart={handlePlaceOrder}
              isOrderPending={createOrderMutation.isPending}
            />
          </TabsContent>

          {/* 내 주문 탭 */}
          <TabsContent value="myorder" className="mt-6">
            <MyOrderTab
              orders={myOrders}
              onDeleteOrder={handleDeleteOrder}
              isDeleting={deleteOrderMutation.isPending}
              isReadOnly={isReadOnly}
            />
          </TabsContent>
        </Tabs>
      </main>

      {/* 라운드 마감 확인 모달 */}
      <ConfirmModal
        isOpen={closeConfirmOpen}
        onClose={() => setCloseConfirmOpen(false)}
        onConfirm={handleCloseConfirm}
        title="정말 라운드를 마감할까요?"
        description="마감하면 메뉴·주문·그룹을 더 이상 수정할 수 없어요."
        confirmText="마감"
        isLoading={statusMutation.isPending}
      />

      {/* 라운드 삭제 확인 모달 */}
      <DeleteConfirmModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        itemName={round.title}
        isLoading={deleteMutation.isPending}
      />

      {/* 라운드 이름 수정 모달 */}
      <RoundEditNameModal
        isOpen={isEditNameOpen}
        onClose={() => setIsEditNameOpen(false)}
        currentTitle={round.title}
        onSubmit={handleEditName}
        isPending={updateRoundMutation.isPending}
      />

      {/* 가게 변경 모달 */}
      <RoundChangeStoreModal
        isOpen={isChangeStoreOpen}
        onClose={() => setIsChangeStoreOpen(false)}
        currentStoreId={round.storeId}
        onSubmit={handleChangeStore}
        isPending={updateRoundMutation.isPending}
      />
    </div>
  );
}
