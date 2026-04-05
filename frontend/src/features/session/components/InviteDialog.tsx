'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import {
  LinkIcon,
  CopyIcon,
  QrCodeIcon,
  ShareIcon,
} from '@/components/common/Icons';

interface InviteDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  sessionTitle: string;
  inviteCode: string;
}

export function InviteDialog({
  open,
  onOpenChange,
  sessionTitle,
  inviteCode,
}: InviteDialogProps) {
  const [copied, setCopied] = useState(false);

  const handleCopyInviteLink = () => {
    const link = `https://dirtypay.app/join/session/${inviteCode}`;
    navigator.clipboard.writeText(link);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleCopyInviteCode = () => {
    navigator.clipboard.writeText(inviteCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>세션에 초대하기</DialogTitle>
          <DialogDescription>
            &ldquo;{sessionTitle}&rdquo;에 다른 사람을 초대하세요
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="link" className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="link">링크 공유</TabsTrigger>
            <TabsTrigger value="code">초대 코드</TabsTrigger>
          </TabsList>

          <TabsContent value="link" className="space-y-4">
            <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
              <LinkIcon className="w-4 h-4 text-muted-foreground shrink-0" />
              <span className="text-sm text-foreground truncate flex-1">
                dirtypay.app/join/session/{inviteCode}
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                onClick={handleCopyInviteLink}
                className="w-full bg-transparent"
              >
                <CopyIcon className="w-4 h-4 mr-2" />
                {copied ? '복사됨!' : '링크 복사'}
              </Button>
              <Button variant="outline" className="w-full bg-transparent">
                <ShareIcon className="w-4 h-4 mr-2" />
                공유하기
              </Button>
            </div>
          </TabsContent>

          <TabsContent value="code" className="space-y-4">
            <div className="flex flex-col items-center gap-4 py-4">
              <div className="w-32 h-32 bg-muted rounded-xl flex items-center justify-center">
                <QrCodeIcon className="w-20 h-20 text-muted-foreground" />
              </div>
              <div className="text-center">
                <p className="text-sm text-muted-foreground mb-1">초대 코드</p>
                <p className="text-2xl font-bold font-mono tracking-wider text-foreground">
                  {inviteCode}
                </p>
              </div>
            </div>
            <Button
              variant="outline"
              onClick={handleCopyInviteCode}
              className="w-full bg-transparent"
            >
              <CopyIcon className="w-4 h-4 mr-2" />
              {copied ? '복사됨!' : '코드 복사'}
            </Button>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}
