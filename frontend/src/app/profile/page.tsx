'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  ChevronLeftIcon,
  UserIcon,
  EditIcon,
  MailIcon,
  CameraIcon,
  LogOutIcon,
  SettingsIcon,
  StoreIcon,
  WalletIcon,
  ChevronRightIcon,
} from '@/components/common/Icons';
import { useAuthContext } from '@/components/providers/auth-provider';
import { useUpdateUserMutation } from '@/features/user';
import { useWalletQuery, WalletBalanceCard, ChargeDialog } from '@/features/wallet';

function WalletMiniSection() {
  const { data: wallet, isLoading } = useWalletQuery();
  const [chargeOpen, setChargeOpen] = useState(false);

  if (isLoading) {
    return <div className="animate-pulse h-16 bg-muted rounded-lg mb-6" />;
  }

  if (!wallet) return null;

  return (
    <div className="mb-6">
      <WalletBalanceCard wallet={wallet} onCharge={() => setChargeOpen(true)} compact />
      <ChargeDialog
        open={chargeOpen}
        onOpenChange={setChargeOpen}
        dailyChargedAmount={wallet.dailyChargedAmount}
      />
    </div>
  );
}

export default function ProfilePage() {
  const { user, isAuthenticated, isLoading, logout, updateUser } = useAuthContext();
  const { mutate: updateUserMutate, isPending: isUpdating } = useUpdateUserMutation();

  const [editNameOpen, setEditNameOpen] = useState(false);
  const [newName, setNewName] = useState('');
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false);

  const handleUpdateName = () => {
    if (!newName.trim() || !user) return;
    updateUserMutate(
      { id: user.id, request: { name: newName.trim() } },
      {
        onSuccess: (data) => {
          setEditNameOpen(false);
          updateUser(data);
        },
      }
    );
  };

  const handleLogout = async () => {
    setLogoutConfirmOpen(false);
    await logout();
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
  };

  // 로딩 중 또는 로그인되지 않은 경우 빈 화면
  // (미들웨어에서 라우트 보호를 처리함)
  if (isLoading || !isAuthenticated || !user) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link
            href="/"
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">내 프로필</h1>
          <Link href="/settings">
            <Button variant="ghost" size="icon">
              <SettingsIcon className="w-5 h-5" />
            </Button>
          </Link>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        {/* 프로필 카드 */}
        <Card className="mb-6">
          <CardContent className="p-6">
            <div className="flex flex-col items-center">
              {/* 프로필 이미지 */}
              <div className="relative mb-4">
                <div className="w-24 h-24 rounded-full bg-primary/10 flex items-center justify-center">
                  {user.profileImage ? (
                    <img
                      src={user.profileImage}
                      alt={user.name}
                      className="w-full h-full rounded-full object-cover"
                    />
                  ) : (
                    <UserIcon className="w-12 h-12 text-primary" />
                  )}
                </div>
                <button
                  className="absolute bottom-0 right-0 w-8 h-8 bg-primary rounded-full flex items-center justify-center shadow-lg hover:bg-primary/90 transition-colors"
                  onClick={() => alert('프로필 사진 변경 기능은 준비 중이에요.')}
                >
                  <CameraIcon className="w-4 h-4 text-primary-foreground" />
                </button>
              </div>

              {/* 이름 */}
              <div className="flex items-center gap-2 mb-1">
                <h2 className="text-xl font-bold text-foreground">{user.name}</h2>
                <button
                  className="p-1 rounded hover:bg-accent transition-colors"
                  onClick={() => {
                    setNewName(user.name);
                    setEditNameOpen(true);
                  }}
                >
                  <EditIcon className="w-4 h-4 text-muted-foreground" />
                </button>
              </div>

              <p className="text-sm text-muted-foreground">
                {formatDate(user.createdDate)} 가입
              </p>
            </div>
          </CardContent>
        </Card>

        {/* 연락처 정보 */}
        <Card className="mb-6">
          <CardContent className="p-4">
            <h3 className="text-sm font-medium text-muted-foreground mb-3">계정 정보</h3>
            <div className="space-y-3">
              <div className="flex items-center gap-3 p-3 bg-muted/30 rounded-lg">
                <MailIcon className="w-5 h-5 text-muted-foreground" />
                <div className="flex-1">
                  <p className="text-xs text-muted-foreground">이메일</p>
                  <p className="text-sm text-foreground">{user.email}</p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 지갑 미니 카드 */}
        <WalletMiniSection />

        {/* 메뉴 */}
        <Card className="mb-6">
          <CardContent className="p-2">
            <Link href="/stores">
              <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-accent transition-colors">
                <div className="flex items-center gap-3">
                  <StoreIcon className="w-5 h-5 text-muted-foreground" />
                  <span className="text-foreground">가게 관리</span>
                </div>
                <ChevronRightIcon className="w-5 h-5 text-muted-foreground" />
              </button>
            </Link>
            <Link href="/settings">
              <button className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-accent transition-colors">
                <div className="flex items-center gap-3">
                  <SettingsIcon className="w-5 h-5 text-muted-foreground" />
                  <span className="text-foreground">설정</span>
                </div>
                <ChevronRightIcon className="w-5 h-5 text-muted-foreground" />
              </button>
            </Link>
          </CardContent>
        </Card>

        {/* 로그아웃 버튼 */}
        <Button
          variant="outline"
          className="w-full bg-transparent text-destructive hover:text-destructive hover:bg-destructive/10"
          onClick={() => setLogoutConfirmOpen(true)}
        >
          <LogOutIcon className="w-4 h-4 mr-2" />
          로그아웃
        </Button>
      </main>

      {/* 이름 수정 다이얼로그 */}
      <Dialog open={editNameOpen} onOpenChange={setEditNameOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>이름 변경</DialogTitle>
            <DialogDescription>새로운 이름을 입력해주세요</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="name">이름</Label>
            <Input
              id="name"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="이름 입력"
              maxLength={50}
            />
            <p className="text-xs text-muted-foreground text-right">
              {newName.length}/50
            </p>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setEditNameOpen(false)}
              className="bg-transparent"
            >
              취소
            </Button>
            <Button
              onClick={handleUpdateName}
              disabled={!newName.trim() || isUpdating}
            >
              {isUpdating ? '변경 중...' : '변경'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 로그아웃 확인 다이얼로그 */}
      <Dialog open={logoutConfirmOpen} onOpenChange={setLogoutConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>로그아웃</DialogTitle>
            <DialogDescription>정말 로그아웃 하시겠어요?</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setLogoutConfirmOpen(false)}
              className="bg-transparent"
            >
              취소
            </Button>
            <Button variant="destructive" onClick={handleLogout}>
              로그아웃
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
