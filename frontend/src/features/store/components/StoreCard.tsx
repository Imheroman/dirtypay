'use client';

import { useRouter } from 'next/navigation';
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { MapPinIcon, EditIcon, TrashIcon } from '@/components/common/Icons';
import { StoreStatusBadge } from './StoreStatusBadge';
import { getStoreTypeLabel } from '../utils';
import type { Store } from '../types';

export interface StoreCardProps {
  store: Store;
  onEdit?: () => void;
  onDelete?: () => void;
}

export function StoreCard({ store, onEdit, onDelete }: StoreCardProps) {
  const router = useRouter();

  const handleCardClick = () => {
    router.push(`/stores/${store.id}`);
  };

  return (
    <Card
      className="cursor-pointer hover:border-primary/30 hover:scale-[1.01] hover:bg-accent/50 hover:shadow-md transition-all duration-200"
      onClick={handleCardClick}
    >
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-lg font-semibold leading-tight">{store.name}</h3>
          <StoreStatusBadge status={store.status} />
        </div>
      </CardHeader>

      <CardContent className="space-y-2 pb-3">
        <div className="flex items-start gap-1.5 text-sm text-muted-foreground">
          <MapPinIcon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          <span>{store.address}</span>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="outline" className="text-xs">
            {getStoreTypeLabel(store.storeType, store.description)}
          </Badge>
        </div>
      </CardContent>

      {(onEdit || onDelete) && (
        <CardFooter className="gap-1 pt-0">
          {onEdit && (
            <Button
              variant="ghost"
              size="sm"
              onClick={(e) => { e.stopPropagation(); onEdit?.(); }}
              aria-label={`${store.name} 매장 수정`}
              className="h-8 gap-1.5 px-2 text-xs text-muted-foreground hover:text-foreground"
            >
              <EditIcon className="h-3.5 w-3.5" aria-hidden="true" />
              수정
            </Button>
          )}
          {onDelete && (
            <Button
              variant="ghost"
              size="sm"
              onClick={(e) => { e.stopPropagation(); onDelete?.(); }}
              aria-label={`${store.name} 매장 삭제`}
              className="h-8 gap-1.5 px-2 text-xs text-muted-foreground hover:text-destructive"
            >
              <TrashIcon className="h-3.5 w-3.5" aria-hidden="true" />
              삭제
            </Button>
          )}
        </CardFooter>
      )}
    </Card>
  );
}
