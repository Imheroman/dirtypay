"use client";

import { useRouter } from "next/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  MapPinIcon,
  UtensilsIcon,
  ReceiptIcon,
  MoreVerticalIcon,
  EditIcon,
  TrashIcon,
} from "@/components/common/Icons";
import type { Store } from "../types";

const statusConfig: Record<
  string,
  { label: string; variant: "default" | "secondary" | "outline" }
> = {
  OPEN: { label: "영업 중", variant: "default" },
  TEMPORARILY_CLOSED: { label: "임시 휴업", variant: "secondary" },
  CLOSED: { label: "운영 종료", variant: "outline" },
};

interface StoreCardProps {
  store: Store;
  onEdit?: (store: Store) => void;
  onDelete?: (store: Store) => void;
}

export function StoreCard({ store, onEdit, onDelete }: StoreCardProps) {
  const router = useRouter();
  const config = statusConfig[store.status] ?? statusConfig.OPEN;
  const hasMenu = onEdit || onDelete;

  const handleCardClick = () => {
    router.push(`/stores/${store.id}`);
  };

  return (
    <Card
      className="hover:shadow-md hover:border-primary/30 hover:scale-[1.01] hover:bg-accent/50 transition-all duration-200 cursor-pointer border-border/60 h-full"
      onClick={handleCardClick}
    >
      <CardContent className="p-5">
        <div className="flex items-start justify-between mb-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <h4 className="font-semibold text-foreground truncate">
                {store.name}
              </h4>
              <Badge variant={config.variant} className="text-xs shrink-0">
                {config.label}
              </Badge>
            </div>
            {store.description && (
              <p className="text-sm text-muted-foreground line-clamp-2">
                {store.description}
              </p>
            )}
          </div>

          {hasMenu && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  className="p-1 rounded-md hover:bg-accent transition-colors shrink-0 ml-2"
                  onClick={(e) => e.stopPropagation()}
                  aria-label="매장 메뉴"
                >
                  <MoreVerticalIcon className="w-5 h-5 text-muted-foreground" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent
                align="end"
                onClick={(e) => e.stopPropagation()}
              >
                {onEdit && (
                  <DropdownMenuItem onClick={() => onEdit(store)}>
                    <EditIcon className="w-4 h-4" />
                    수정
                  </DropdownMenuItem>
                )}
                {onEdit && onDelete && <DropdownMenuSeparator />}
                {onDelete && (
                  <DropdownMenuItem
                    className="text-destructive focus:text-destructive"
                    onClick={() => onDelete(store)}
                  >
                    <TrashIcon className="w-4 h-4" />
                    삭제
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>

        <div className="flex items-center gap-1 text-sm text-muted-foreground mb-3">
          <MapPinIcon className="w-4 h-4 shrink-0" />
          <span className="truncate">{store.address}</span>
        </div>

        <div className="flex items-center gap-4 text-sm text-muted-foreground pt-3 border-t border-border/50">
          <div className="flex items-center gap-1">
            <UtensilsIcon className="w-4 h-4" />
            <span>{store.menuCount ?? 0}개 메뉴</span>
          </div>
          <div className="flex items-center gap-1">
            <ReceiptIcon className="w-4 h-4" />
            <span>{store.orderCount ?? 0}건 주문</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
