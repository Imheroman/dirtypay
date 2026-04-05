'use client';

import Link from 'next/link';
import Image from 'next/image';
import { usePathname, useParams } from 'next/navigation';
import { Logo, UserIcon, BellIcon, LogInIcon, MenuIcon } from './Icons';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useAuthContext } from '@/components/providers/auth-provider';
import { useUIStore } from '@/store/useUIStore';
import { useSessionQuery } from '@/features/session/hooks/useSessionQuery';
import { Navigation } from './Navigation';

export function Header() {
  const pathname = usePathname();
  const params = useParams();
  const sessionId = params?.sessionId as string | undefined;
  const { isAuthenticated, user } = useAuthContext();
  const setSidebarOpen = useUIStore((state) => state.setSidebarOpen);
  const { data: session } = useSessionQuery(Number(sessionId), {
    enabled: !!sessionId,
  });

  const isOwner = session?.ownerId === user?.id;

  // 세션 내부인지 확인
  const isInSession = pathname?.startsWith('/sessions/') && sessionId;

  const navItems = sessionId
    ? [
        { href: `/sessions/${sessionId}`, label: '홈', exact: true },
        ...(isOwner
          ? [{ href: `/sessions/${sessionId}/organization`, label: '조직 관리' }]
          : []),
        { href: `/sessions/${sessionId}/settlement`, label: '정산' },
      ]
    : [];

  const isActive = (href: string, exact?: boolean) => {
    if (!pathname) return false;
    if (exact) return pathname === href;
    return pathname.startsWith(href);
  };

  return (
    <header className="sticky top-0 z-50 w-full bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
      <div className="container mx-auto px-6 max-w-5xl h-14 md:h-16 flex items-center justify-between">
        {/* 왼쪽: 햄버거 메뉴 (모바일) + 로고 */}
        <div className="flex items-center gap-1 shrink-0">
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden text-muted-foreground hover:text-foreground"
            onClick={() => setSidebarOpen(true)}
            aria-label="메뉴 열기"
          >
            <MenuIcon className="h-5 w-5" />
          </Button>
          <Link href="/" className="flex items-center gap-2">
          <Logo className="h-7 w-7 md:h-8 md:w-8" />
          <span className="font-semibold text-base md:text-lg text-foreground">
            Dirty Pay
          </span>
          </Link>
        </div>

        {/* 가운데: 네비게이션 (세션 내부일 때만, 데스크톱) */}
        {isInSession && navItems.length > 0 && (
          <nav className="hidden md:flex items-center gap-1">
            {navItems.map((item) => {
              const active = isActive(item.href, item.exact);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
                    active
                      ? 'bg-primary/10 text-primary'
                      : 'text-muted-foreground hover:text-foreground hover:bg-accent'
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        )}

        {/* 오른쪽: 액션 버튼들 */}
        <div className="flex items-center gap-1">
          {isAuthenticated ? (
            <>
              <Button
                variant="ghost"
                size="icon"
                className="text-muted-foreground hover:text-foreground"
                aria-label="알림"
              >
                <BellIcon className="h-5 w-5" />
              </Button>
              <Link href="/profile">
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-muted-foreground hover:text-foreground"
                  aria-label="프로필"
                >
                  {user?.profileImage ? (
                    <Image
                      src={user.profileImage}
                      alt={user.name ?? '프로필'}
                      width={24}
                      height={24}
                      className="rounded-full object-cover"
                    />
                  ) : (
                    <UserIcon className="h-5 w-5" />
                  )}
                </Button>
              </Link>
            </>
          ) : (
            <Link href="/login">
              <Button size="sm">
                <LogInIcon className="h-4 w-4 mr-1.5" />
                로그인
              </Button>
            </Link>
          )}
        </div>
      </div>

      {/* 모바일 네비게이션 (세션 내부일 때만) */}
      {isInSession && navItems.length > 0 && (
        <nav className="md:hidden flex items-center gap-1.5 px-6 pb-3 overflow-x-auto scrollbar-hide">
          {navItems.map((item) => {
            const active = isActive(item.href, item.exact);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'px-3 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors',
                  active
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      )}

      {/* 모바일 사이드 메뉴 */}
      <Navigation />
    </header>
  );
}
