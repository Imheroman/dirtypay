'use client';

import Link from 'next/link';
import { usePathname, useParams } from 'next/navigation';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { Logo, HomeIcon, UserIcon, SettingsIcon, LogInIcon, UserPlusIcon, UsersIcon, WalletIcon, StoreIcon } from './Icons';
import { cn } from '@/lib/utils';
import { useUIStore } from '@/store/useUIStore';
import { useAuthContext } from '@/components/providers/auth-provider';
import { useSessionQuery } from '@/features/session/hooks/useSessionQuery';

interface NavItem {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  exact?: boolean;
}

export function Navigation() {
  const pathname = usePathname();
  const params = useParams();
  const sessionId = params?.sessionId as string | undefined;
  const { sidebarOpen, setSidebarOpen } = useUIStore();
  const { isAuthenticated, user } = useAuthContext();
  const { data: session } = useSessionQuery(Number(sessionId), {
    enabled: !!sessionId,
  });

  const isOwner = session?.ownerId === user?.id;

  const isInSession = pathname?.startsWith('/sessions/') && sessionId;

  const isActive = (href: string, exact?: boolean) => {
    if (!pathname) return false;
    if (exact) return pathname === href;
    return pathname.startsWith(href);
  };

  const handleLinkClick = () => {
    setSidebarOpen(false);
  };

  const sessionNavItems: NavItem[] = sessionId
    ? [
        { href: `/sessions/${sessionId}`, label: '홈', icon: HomeIcon, exact: true },
        ...(isOwner
          ? [{ href: `/sessions/${sessionId}/organization`, label: '조직 관리', icon: UsersIcon }]
          : []),
        { href: `/sessions/${sessionId}/settlement`, label: '정산', icon: WalletIcon },
      ]
    : [];

  const authNavItems: NavItem[] = [
    { href: '/', label: '홈', icon: HomeIcon, exact: true },
    { href: '/wallet', label: '지갑', icon: WalletIcon },
    { href: '/stores', label: '가게 관리', icon: StoreIcon },
    { href: '/profile', label: '프로필', icon: UserIcon },
    { href: '/settings', label: '설정', icon: SettingsIcon },
  ];

  const guestNavItems: NavItem[] = [
    { href: '/', label: '홈', icon: HomeIcon, exact: true },
    { href: '/login', label: '로그인', icon: LogInIcon },
    { href: '/signup', label: '회원가입', icon: UserPlusIcon },
  ];

  const mainNavItems = isAuthenticated ? authNavItems : guestNavItems;

  return (
    <Sheet open={sidebarOpen} onOpenChange={setSidebarOpen}>
      <SheetContent side="left" className="w-72 p-0">
        <SheetHeader className="border-b border-border px-4 py-4">
          <SheetTitle className="flex items-center gap-2">
            <Logo className="h-7 w-7" />
            <span className="font-semibold text-base">Dirty Pay</span>
          </SheetTitle>
        </SheetHeader>

        <nav className="flex-1 overflow-y-auto py-2">
          {/* 메인 네비게이션 */}
          <div className="px-2">
            {mainNavItems.map((item) => {
              const active = isActive(item.href, item.exact);
              const Icon = item.icon;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={handleLinkClick}
                  className={cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                    active
                      ? 'bg-primary/10 text-primary'
                      : 'text-muted-foreground hover:text-foreground hover:bg-accent'
                  )}
                >
                  <Icon className="h-5 w-5" />
                  {item.label}
                </Link>
              );
            })}
          </div>

          {/* 세션 내부 네비게이션 */}
          {isInSession && sessionNavItems.length > 0 && (
            <>
              <div className="mx-4 my-3 border-t border-border" />
              <div className="px-2">
                <p className="px-3 mb-1 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                  세션 메뉴
                </p>
                {sessionNavItems.map((item) => {
                  const active = isActive(item.href, item.exact);
                  const Icon = item.icon;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={handleLinkClick}
                      className={cn(
                        'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                        active
                          ? 'bg-primary/10 text-primary'
                          : 'text-muted-foreground hover:text-foreground hover:bg-accent'
                      )}
                    >
                      <Icon className="h-5 w-5" />
                      {item.label}
                    </Link>
                  );
                })}
              </div>
            </>
          )}
        </nav>
      </SheetContent>
    </Sheet>
  );
}
