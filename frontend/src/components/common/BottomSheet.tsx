'use client';

import { useCallback, useEffect, useRef } from 'react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';

const DRAG_CLOSE_THRESHOLD = 80;

interface BottomSheetProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
}

export function BottomSheet({
  isOpen,
  onClose,
  title,
  description,
  children,
  footer,
}: BottomSheetProps) {
  const contentRef = useRef<HTMLDivElement>(null);
  const dragState = useRef({ startY: 0, deltaY: 0, isDragging: false });

  useEffect(() => {
    if (isOpen && contentRef.current) {
      contentRef.current.style.transform = '';
      contentRef.current.style.transition = '';
    }
  }, [isOpen]);

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    dragState.current = { startY: e.touches[0].clientY, deltaY: 0, isDragging: true };
    if (contentRef.current) {
      contentRef.current.style.transition = 'none';
    }
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (!dragState.current.isDragging) return;
    const deltaY = e.touches[0].clientY - dragState.current.startY;
    if (deltaY < 0) return;
    dragState.current.deltaY = deltaY;
    if (contentRef.current) {
      contentRef.current.style.transform = `translateY(${deltaY}px)`;
    }
  }, []);

  const handleTouchEnd = useCallback(() => {
    if (!dragState.current.isDragging) return;
    dragState.current.isDragging = false;

    if (contentRef.current) {
      contentRef.current.style.transition = 'transform 0.2s ease-out';
      if (dragState.current.deltaY > DRAG_CLOSE_THRESHOLD) {
        contentRef.current.style.transform = 'translateY(100%)';
        setTimeout(onClose, 200);
      } else {
        contentRef.current.style.transform = 'translateY(0)';
      }
    }
  }, [onClose]);

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent ref={contentRef} side="bottom" hideCloseButton>
        <div
          className="absolute inset-x-0 top-0 z-10 h-8 cursor-grab touch-none active:cursor-grabbing"
          aria-hidden="true"
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onTouchEnd={handleTouchEnd}
        />
        {(title || description) ? (
          <SheetHeader>
            {title && <SheetTitle>{title}</SheetTitle>}
            {description && <SheetDescription>{description}</SheetDescription>}
          </SheetHeader>
        ) : (
          <SheetTitle className="sr-only">확인</SheetTitle>
        )}
        <div className="flex-1 overflow-y-auto px-4">{children}</div>
        {footer && <SheetFooter>{footer}</SheetFooter>}
      </SheetContent>
    </Sheet>
  );
}

interface ActionBottomSheetProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  primaryAction?: {
    label: string;
    onClick: () => void;
    isLoading?: boolean;
    disabled?: boolean;
  };
  secondaryAction?: {
    label: string;
    onClick: () => void;
  };
}

export function ActionBottomSheet({
  isOpen,
  onClose,
  title,
  description,
  children,
  primaryAction,
  secondaryAction,
}: ActionBottomSheetProps) {
  return (
    <BottomSheet
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      description={description}
      footer={
        (primaryAction || secondaryAction) && (
          <div className="flex flex-col gap-2 w-full">
            {primaryAction && (
              <Button
                onClick={primaryAction.onClick}
                disabled={primaryAction.disabled || primaryAction.isLoading}
                className="w-full"
              >
                {primaryAction.isLoading ? '처리 중...' : primaryAction.label}
              </Button>
            )}
            {secondaryAction && (
              <Button
                variant="outline"
                onClick={secondaryAction.onClick}
                className="w-full"
              >
                {secondaryAction.label}
              </Button>
            )}
          </div>
        )
      }
    >
      {children}
    </BottomSheet>
  );
}
