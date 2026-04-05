'use client';

import { useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { CopyIcon, CheckIcon, QrCodeIcon } from './Icons';
import { cn } from '@/lib/utils';

interface QRCodeDisplayProps {
  /** QR 코드에 인코딩될 값 (URL 또는 초대 코드) */
  value: string;
  /** QR 코드 크기 (px) */
  size?: number;
  /** 표시할 제목 */
  title?: string;
  /** 하단에 표시할 설명 */
  description?: string;
  /** 복사 버튼 표시 여부 */
  showCopyButton?: boolean;
  /** 카드 스타일 사용 여부 */
  withCard?: boolean;
  /** 추가 className */
  className?: string;
}

/**
 * QR 코드 표시 컴포넌트 (UI Mock)
 * 실제 QR 코드 생성은 추후 qrcode.react 등의 라이브러리로 대체
 */
export function QRCodeDisplay({
  value,
  size = 200,
  title,
  description,
  showCopyButton = true,
  withCard = true,
  className,
}: QRCodeDisplayProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      console.error('복사 실패');
    }
  };

  const QRContent = (
    <div className={cn('flex flex-col items-center', className)}>
      {title && (
        <h3 className="text-lg font-semibold text-foreground mb-4">{title}</h3>
      )}

      {/* Mock QR 코드 */}
      <div
        className="relative bg-white p-4 rounded-xl shadow-sm border border-border"
        style={{ width: size + 32, height: size + 32 }}
      >
        {/* QR 코드 패턴 (Mock) */}
        <div
          className="w-full h-full bg-gradient-to-br from-foreground/5 to-foreground/10 rounded-lg flex items-center justify-center relative overflow-hidden"
          style={{ width: size, height: size }}
        >
          {/* 간단한 QR 패턴 시뮬레이션 */}
          <div className="absolute inset-0 grid grid-cols-7 grid-rows-7 gap-0.5 p-2">
            {/* Position Detection Pattern - Top Left */}
            <div className="col-span-2 row-span-2 bg-foreground rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/60 rounded-sm" />
            <div className="bg-transparent" />
            {/* Position Detection Pattern - Top Right */}
            <div className="col-span-2 row-span-2 bg-foreground rounded-sm" />

            {/* Row 2 */}
            <div className="bg-foreground/40 rounded-sm" />
            <div className="bg-foreground/60 rounded-sm" />
            <div className="bg-transparent" />

            {/* Row 3 */}
            <div className="bg-transparent" />
            <div className="bg-foreground/50 rounded-sm" />
            <div className="bg-foreground/30 rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/60 rounded-sm" />
            <div className="bg-foreground/40 rounded-sm" />
            <div className="bg-transparent" />

            {/* Row 4 */}
            <div className="bg-foreground/60 rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/40 rounded-sm" />
            <div className="bg-foreground/70 rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/50 rounded-sm" />
            <div className="bg-foreground/30 rounded-sm" />

            {/* Row 5 */}
            <div className="bg-transparent" />
            <div className="bg-foreground/50 rounded-sm" />
            <div className="bg-foreground/60 rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/40 rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/50 rounded-sm" />

            {/* Position Detection Pattern - Bottom Left */}
            <div className="col-span-2 row-span-2 bg-foreground rounded-sm" />
            <div className="bg-foreground/40 rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/60 rounded-sm" />
            <div className="bg-foreground/30 rounded-sm" />
            <div className="bg-transparent" />

            {/* Last Row */}
            <div className="bg-transparent" />
            <div className="bg-foreground/50 rounded-sm" />
            <div className="bg-transparent" />
            <div className="bg-foreground/40 rounded-sm" />
            <div className="bg-foreground/60 rounded-sm" />
          </div>

          {/* 중앙 로고 영역 */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="w-12 h-12 bg-background rounded-lg shadow-sm flex items-center justify-center">
              <QrCodeIcon className="w-6 h-6 text-primary" />
            </div>
          </div>
        </div>
      </div>

      {/* 코드 값 표시 */}
      <div className="mt-4 flex items-center gap-2">
        <code className="px-3 py-1.5 bg-muted rounded-lg text-sm font-mono text-foreground">
          {value.length > 20 ? `${value.slice(0, 20)}...` : value}
        </code>
        {showCopyButton && (
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={handleCopy}
          >
            {copied ? (
              <CheckIcon className="w-4 h-4 text-green-500" />
            ) : (
              <CopyIcon className="w-4 h-4" />
            )}
          </Button>
        )}
      </div>

      {description && (
        <p className="text-sm text-muted-foreground text-center mt-2 max-w-[250px]">
          {description}
        </p>
      )}
    </div>
  );

  if (withCard) {
    return (
      <Card>
        <CardContent className="p-6">{QRContent}</CardContent>
      </Card>
    );
  }

  return QRContent;
}

/**
 * 초대 코드 QR 다이얼로그용 컴포넌트
 */
interface InviteQRCodeProps {
  inviteCode: string;
  sessionTitle?: string;
}

export function InviteQRCode({ inviteCode, sessionTitle }: InviteQRCodeProps) {
  return (
    <QRCodeDisplay
      value={inviteCode}
      title={sessionTitle ? `${sessionTitle} 초대` : '세션 초대'}
      description="QR 코드를 스캔하거나 초대 코드를 입력해서 참여할 수 있어요"
      size={180}
    />
  );
}
