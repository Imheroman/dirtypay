'use client';

import { Button } from '@/components/ui/button';
import { AlertCircleIcon, RefreshIcon } from './Icons';

interface ErrorFallbackProps {
  error: Error | null;
  onReset: () => void;
}

export function ErrorFallback({ error, onReset }: ErrorFallbackProps) {
  const isDev = process.env.NODE_ENV === 'development';

  return (
    <div
      role="alert"
      className="flex flex-col items-center justify-center gap-4 py-12 text-center"
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
        <AlertCircleIcon className="h-6 w-6 text-destructive" />
      </div>
      <div className="space-y-1">
        <h3 className="text-base font-medium text-foreground">문제가 생겼어요</h3>
        <p className="text-sm text-muted-foreground">잠시 후 다시 시도해 주세요.</p>
      </div>
      <Button variant="outline" size="sm" onClick={onReset}>
        <RefreshIcon className="h-4 w-4" />
        다시 시도
      </Button>
      {isDev && error?.stack && (
        <details className="mt-2 w-full max-w-lg text-left">
          <summary className="cursor-pointer text-xs text-muted-foreground hover:text-foreground">
            오류 상세 보기 (개발 환경)
          </summary>
          <pre className="mt-2 overflow-auto rounded-md bg-muted p-3 text-xs text-destructive">
            {error.stack}
          </pre>
        </details>
      )}
    </div>
  );
}
