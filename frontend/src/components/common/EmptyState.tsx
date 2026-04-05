'use client';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { PlusIcon } from './Icons';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

export function EmptyState({
  icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex min-w-0 flex-1 flex-col items-center justify-center gap-4 rounded-lg border border-dashed p-6 text-center md:p-12',
        className
      )}
    >
      {icon && (
        <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-muted">
          {icon}
        </div>
      )}
      <div className="space-y-1">
        <h3 className="text-base font-medium text-foreground">{title}</h3>
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {action && (
        <Button onClick={action.onClick} size="sm">
          <PlusIcon className="h-4 w-4" />
          {action.label}
        </Button>
      )}
    </div>
  );
}

interface EmptyListProps {
  message?: string;
  className?: string;
}

export function EmptyList({
  message = '아직 등록된 정보가 없어요',
  className,
}: EmptyListProps) {
  return (
    <div
      className={cn(
        'flex items-center justify-center py-8 text-sm text-muted-foreground',
        className
      )}
    >
      {message}
    </div>
  );
}
