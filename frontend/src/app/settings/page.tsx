'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useTheme } from 'next-themes';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import {
  ChevronLeftIcon,
  SunIcon,
  MoonIcon,
  MonitorIcon,
  BellIcon,
  InfoIcon,
  ChevronRightIcon,
} from '@/components/common/Icons';
import { cn } from '@/lib/utils';

type ThemeOption = 'system' | 'light' | 'dark';

export default function SettingsPage() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  // 알림 설정 (Mock)
  const [pushNotification, setPushNotification] = useState(true);
  const [settlementReminder, setSettlementReminder] = useState(true);
  const [newRoundAlert, setNewRoundAlert] = useState(true);

  // hydration 이슈 방지
  useEffect(() => {
    setMounted(true);
  }, []);

  const themeOptions: { value: ThemeOption; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { value: 'system', label: '시스템 설정', icon: MonitorIcon },
    { value: 'light', label: '라이트 모드', icon: SunIcon },
    { value: 'dark', label: '다크 모드', icon: MoonIcon },
  ];

  if (!mounted) {
    return null;
  }

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link
            href="/profile"
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">설정</h1>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg space-y-6">
        {/* 테마 설정 */}
        <section>
          <h2 className="text-sm font-medium text-muted-foreground mb-3 px-1">테마</h2>
          <Card>
            <CardContent className="p-2">
              <div className="grid grid-cols-3 gap-2">
                {themeOptions.map((option) => {
                  const Icon = option.icon;
                  const isSelected = theme === option.value;

                  return (
                    <button
                      key={option.value}
                      onClick={() => setTheme(option.value)}
                      className={cn(
                        'flex flex-col items-center gap-2 p-4 rounded-xl transition-all',
                        isSelected
                          ? 'bg-primary text-primary-foreground'
                          : 'hover:bg-accent'
                      )}
                    >
                      <Icon className="w-6 h-6" />
                      <span className="text-xs font-medium">{option.label}</span>
                    </button>
                  );
                })}
              </div>
            </CardContent>
          </Card>
          <p className="text-xs text-muted-foreground mt-2 px-1">
            시스템 설정을 선택하면 기기의 테마 설정을 따라갑니다
          </p>
        </section>

        {/* 알림 설정 */}
        <section>
          <h2 className="text-sm font-medium text-muted-foreground mb-3 px-1">알림</h2>
          <Card>
            <CardContent className="p-4 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center">
                    <BellIcon className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <Label htmlFor="push" className="text-foreground font-medium">
                      푸시 알림
                    </Label>
                    <p className="text-xs text-muted-foreground">
                      앱 알림을 받습니다
                    </p>
                  </div>
                </div>
                <Switch
                  id="push"
                  checked={pushNotification}
                  onCheckedChange={setPushNotification}
                />
              </div>

              <div className="border-t border-border pt-4">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <Label htmlFor="settlement" className="text-foreground">
                      정산 알림
                    </Label>
                    <p className="text-xs text-muted-foreground">
                      정산 완료/미완료 알림
                    </p>
                  </div>
                  <Switch
                    id="settlement"
                    checked={settlementReminder}
                    onCheckedChange={setSettlementReminder}
                    disabled={!pushNotification}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <Label htmlFor="newRound" className="text-foreground">
                      새 라운드 알림
                    </Label>
                    <p className="text-xs text-muted-foreground">
                      새 라운드 생성 시 알림
                    </p>
                  </div>
                  <Switch
                    id="newRound"
                    checked={newRoundAlert}
                    onCheckedChange={setNewRoundAlert}
                    disabled={!pushNotification}
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        {/* 앱 정보 */}
        <section>
          <h2 className="text-sm font-medium text-muted-foreground mb-3 px-1">앱 정보</h2>
          <Card>
            <CardContent className="p-2">
              <button
                className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-accent transition-colors"
                onClick={() => alert('이용약관 페이지는 준비 중이에요.')}
              >
                <div className="flex items-center gap-3">
                  <InfoIcon className="w-5 h-5 text-muted-foreground" />
                  <span className="text-foreground">이용약관</span>
                </div>
                <ChevronRightIcon className="w-5 h-5 text-muted-foreground" />
              </button>

              <button
                className="w-full flex items-center justify-between p-3 rounded-lg hover:bg-accent transition-colors"
                onClick={() => alert('개인정보처리방침 페이지는 준비 중이에요.')}
              >
                <div className="flex items-center gap-3">
                  <InfoIcon className="w-5 h-5 text-muted-foreground" />
                  <span className="text-foreground">개인정보처리방침</span>
                </div>
                <ChevronRightIcon className="w-5 h-5 text-muted-foreground" />
              </button>

              <div className="flex items-center justify-between p-3">
                <div className="flex items-center gap-3">
                  <InfoIcon className="w-5 h-5 text-muted-foreground" />
                  <span className="text-foreground">버전</span>
                </div>
                <span className="text-sm text-muted-foreground">1.0.0</span>
              </div>
            </CardContent>
          </Card>
        </section>
      </main>
    </div>
  );
}
