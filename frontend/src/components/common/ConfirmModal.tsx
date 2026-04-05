'use client';

import { useId } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { AlertCircleIcon } from './Icons';
import { BottomSheet } from './BottomSheet';
import { useIsMobile } from '@/hooks/use-media-query';

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  showCancel?: boolean;
  variant?: 'default' | 'destructive';
  isLoading?: boolean;
}

export function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmText = '확인',
  cancelText = '취소',
  showCancel = true,
  variant = 'default',
  isLoading = false,
}: ConfirmModalProps) {
  const id = useId();
  const titleId = `${id}-title`;
  const descriptionId = `${id}-description`;
  const isMobile = useIsMobile();

  const handleConfirm = () => {
    onConfirm();
  };

  const destructiveIcon = variant === 'destructive' && (
    <div className="mx-auto mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-destructive/10 sm:mx-0">
      <AlertCircleIcon className="h-5 w-5 text-destructive" />
    </div>
  );

  const footerButtons = (
    <>
      {showCancel && (
        <Button
          variant="outline"
          onClick={onClose}
          disabled={isLoading}
          className={isMobile ? 'w-full' : ''}
        >
          {cancelText}
        </Button>
      )}
      <Button
        variant={variant === 'destructive' ? 'destructive' : 'default'}
        onClick={handleConfirm}
        disabled={isLoading}
        className={isMobile ? 'w-full' : ''}
      >
        {isLoading ? '처리 중...' : confirmText}
      </Button>
    </>
  );

  if (isMobile) {
    return (
      <BottomSheet
        isOpen={isOpen}
        onClose={onClose}
        footer={
          <div className="flex flex-col-reverse gap-2 w-full">
            {footerButtons}
          </div>
        }
      >
        <div className="text-center" role="alertdialog" aria-labelledby={titleId} aria-describedby={description ? descriptionId : undefined}>
          {destructiveIcon}
          <h2 id={titleId} className="text-lg font-semibold leading-none tracking-tight">
            {title}
          </h2>
          {description && (
            <p id={descriptionId} className="mt-1.5 text-sm text-muted-foreground">
              {description}
            </p>
          )}
        </div>
      </BottomSheet>
    );
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent
        className="max-w-[calc(100%-2rem)] sm:max-w-md"
        {...(!description && { 'aria-describedby': undefined })}
      >
        <DialogHeader className="text-center sm:text-left">
          {destructiveIcon}
          <DialogTitle>{title}</DialogTitle>
          {description && (
            <DialogDescription>{description}</DialogDescription>
          )}
        </DialogHeader>
        <DialogFooter className="gap-2 sm:gap-0">
          {footerButtons}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

interface DeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  itemName?: string;
  isLoading?: boolean;
}

export function DeleteConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  itemName,
  isLoading = false,
}: DeleteConfirmModalProps) {
  return (
    <ConfirmModal
      isOpen={isOpen}
      onClose={onClose}
      onConfirm={onConfirm}
      title="정말 삭제할까요?"
      description={
        itemName
          ? `'${itemName}'을(를) 삭제하면 되돌릴 수 없어요.`
          : '삭제하면 되돌릴 수 없어요.'
      }
      confirmText="삭제"
      cancelText="취소"
      variant="destructive"
      isLoading={isLoading}
    />
  );
}
