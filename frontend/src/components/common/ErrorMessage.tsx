'use client';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { AlertCircleIcon, RefreshIcon } from './Icons';

interface ErrorMessageProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorMessage({
  title = '문제가 생겼어요',
  message = '다시 시도해 주세요.',
  onRetry,
  className,
}: ErrorMessageProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-4 py-12 text-center',
        className
      )}
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
        <AlertCircleIcon className="h-6 w-6 text-destructive" />
      </div>
      <div className="space-y-1">
        <h3 className="text-base font-medium text-foreground">{title}</h3>
        <p className="text-sm text-muted-foreground">{message}</p>
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshIcon className="h-4 w-4" />
          다시 시도
        </Button>
      )}
    </div>
  );
}

interface InlineErrorProps {
  message: string;
  className?: string;
}

export function InlineError({ message, className }: InlineErrorProps) {
  return (
    <div
      className={cn(
        'flex items-center gap-2 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive',
        className
      )}
    >
      <AlertCircleIcon className="h-4 w-4 shrink-0" />
      <span>{message}</span>
    </div>
  );
}
