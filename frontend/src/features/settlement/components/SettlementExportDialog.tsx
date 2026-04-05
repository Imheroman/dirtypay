'use client';

import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  ImageIcon,
  CopyIcon,
  MessageCircleIcon,
  CheckIcon,
  FileTextIcon,
} from '@/components/common/Icons';
import { cn } from '@/lib/utils';
import { formatAmount } from '@/lib/format';
import { toast } from 'sonner';
import type { MemberAmount, RoundSettlementSummary } from '../types';

interface SettlementExportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  sessionTitle: string;
  totalAmount: number;
  memberSettlements: MemberAmount[];
  roundSummaries: RoundSettlementSummary[];
}

type ExportOption = 'image' | 'text' | 'kakao';

export function SettlementExportDialog({
  open,
  onOpenChange,
  sessionTitle,
  totalAmount,
  memberSettlements,
  roundSummaries,
}: SettlementExportDialogProps) {
  const [copiedText, setCopiedText] = useState(false);
  const [selectedOption, setSelectedOption] = useState<ExportOption | null>(null);

  const generateTextSummary = () => {
    const lines: string[] = [];
    lines.push(`[${sessionTitle}] 정산 내역`);
    lines.push('');
    lines.push(`총 금액: ${formatAmount(totalAmount)}원`);
    lines.push(`라운드: ${roundSummaries.length}개`);
    lines.push(`참여자: ${memberSettlements.length}명`);
    lines.push('');
    lines.push('--- 멤버별 정산 ---');

    memberSettlements.forEach((member) => {
      const status = member.isPaid ? '(완료)' : '(대기)';
      lines.push(`${member.nickname}: ${formatAmount(member.amount)}원 ${status}`);
    });

    lines.push('');
    lines.push('--- 라운드별 정산 ---');

    roundSummaries.forEach((round) => {
      lines.push(`라운드 ${round.roundId}: ${formatAmount(round.totalAmount)}원`);
    });

    return lines.join('\n');
  };

  const handleCopyText = async () => {
    const text = generateTextSummary();
    try {
      await navigator.clipboard.writeText(text);
      setCopiedText(true);
      toast.success('정산 내역을 복사했어요');
      setTimeout(() => setCopiedText(false), 2000);
    } catch {
      toast.error('복사에 실패했어요. 다시 시도해 주세요.');
    }
  };

  const handleExportImage = () => {
    setSelectedOption('image');
    console.log('이미지 내보내기');
  };

  const handleShareKakao = () => {
    setSelectedOption('kakao');
    console.log('카카오톡 공유');
  };

  const exportOptions = [
    {
      id: 'image' as ExportOption,
      icon: ImageIcon,
      title: '이미지로 저장',
      description: '정산 내역을 이미지로 저장해요',
      onClick: handleExportImage,
    },
    {
      id: 'text' as ExportOption,
      icon: copiedText ? CheckIcon : CopyIcon,
      title: copiedText ? '복사 완료!' : '텍스트 복사',
      description: '정산 내역을 텍스트로 복사해요',
      onClick: handleCopyText,
    },
    {
      id: 'kakao' as ExportOption,
      icon: MessageCircleIcon,
      title: '카카오톡 공유',
      description: '카카오톡으로 바로 공유해요',
      onClick: handleShareKakao,
    },
  ];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm mx-auto">
        <DialogHeader>
          <DialogTitle className="text-center">정산 내역 공유하기</DialogTitle>
        </DialogHeader>

        <div className="space-y-3 mt-4">
          {exportOptions.map((option) => {
            const Icon = option.icon;
            const isSelected = selectedOption === option.id;
            const isCopied = option.id === 'text' && copiedText;

            return (
              <Card
                key={option.id}
                className={cn(
                  'cursor-pointer transition-all hover:bg-accent/50',
                  isSelected && 'border-primary bg-primary/5',
                  isCopied && 'border-green-500 bg-green-50'
                )}
                onClick={option.onClick}
              >
                <CardContent className="p-4">
                  <div className="flex items-center gap-4">
                    <div
                      className={cn(
                        'w-10 h-10 rounded-full flex items-center justify-center',
                        isCopied
                          ? 'bg-green-100 text-green-600'
                          : 'bg-secondary text-secondary-foreground'
                      )}
                    >
                      <Icon className="w-5 h-5" />
                    </div>
                    <div className="flex-1">
                      <p className="font-medium text-foreground">{option.title}</p>
                      <p className="text-sm text-muted-foreground">
                        {option.description}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>

        {/* 미리보기 섹션 */}
        <div className="mt-6">
          <p className="text-sm text-muted-foreground mb-2">미리보기</p>
          <Card className="bg-muted/30">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 mb-3">
                <FileTextIcon className="w-4 h-4 text-muted-foreground" />
                <span className="text-sm font-medium text-foreground">
                  {sessionTitle}
                </span>
              </div>
              <div className="space-y-1 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">총 금액</span>
                  <span className="font-semibold text-foreground">
                    {formatAmount(totalAmount)}원
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">참여자</span>
                  <span className="text-foreground">{memberSettlements.length}명</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">라운드</span>
                  <span className="text-foreground">{roundSummaries.length}개</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <Button
          variant="outline"
          className="w-full mt-4 bg-transparent"
          onClick={() => onOpenChange(false)}
        >
          닫기
        </Button>
      </DialogContent>
    </Dialog>
  );
}
