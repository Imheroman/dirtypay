"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { createMenuSchema, type CreateMenuFormData } from "../schemas";
import { useCreateMenuMutation, useUpdateMenuMutation } from "../hooks";
import type { Menu } from "../types";

interface MenuFormProps {
  storeId: number;
  menu?: Menu;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function MenuForm({ storeId, menu, open, onOpenChange }: MenuFormProps) {
  const isEdit = !!menu;
  const createMutation = useCreateMenuMutation();
  const updateMutation = useUpdateMenuMutation();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, isValid },
  } = useForm<CreateMenuFormData>({
    resolver: zodResolver(createMenuSchema),
    mode: "onBlur",
    reValidateMode: "onChange",
    defaultValues: {
      name: "",
      price: 0,
      description: "",
      category: "",
      imageUrl: "",
      isAvailable: true,
    },
  });

  const isAvailable = watch("isAvailable");

  useEffect(() => {
    if (open && menu) {
      reset({
        name: menu.name,
        price: menu.price,
        description: menu.description ?? "",
        category: menu.category ?? "",
        imageUrl: menu.imageUrl ?? "",
        isAvailable: menu.isAvailable,
      });
    } else if (open && !menu) {
      reset({
        name: "",
        price: 0,
        description: "",
        category: "",
        imageUrl: "",
        isAvailable: true,
      });
    }
  }, [open, menu, reset]);

  const onSubmit = (data: CreateMenuFormData) => {
    const request = {
      name: data.name,
      price: data.price,
      description: data.description || undefined,
      category: data.category || undefined,
      imageUrl: data.imageUrl || undefined,
      isAvailable: data.isAvailable,
    };

    if (isEdit && menu) {
      updateMutation.mutate(
        { menuId: menu.id, storeId, request },
        { onSuccess: () => onOpenChange(false) },
      );
    } else {
      createMutation.mutate(
        { storeId, request },
        { onSuccess: () => onOpenChange(false) },
      );
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? "메뉴 수정" : "메뉴 추가"}</DialogTitle>
          <DialogDescription>
            {isEdit ? "메뉴 정보를 수정해 주세요" : "새 메뉴를 추가해 주세요"}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="menu-name">메뉴 이름</Label>
            <Input
              id="menu-name"
              placeholder="메뉴 이름"
              aria-invalid={!!errors.name}
              {...register("name")}
            />
            {errors.name && (
              <p className="text-sm text-destructive">{errors.name.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-price">가격 (원)</Label>
            <Input
              id="menu-price"
              type="number"
              placeholder="0"
              aria-invalid={!!errors.price}
              {...register("price", { valueAsNumber: true })}
            />
            {errors.price && (
              <p className="text-sm text-destructive">{errors.price.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-description">설명 (선택)</Label>
            <Input
              id="menu-description"
              placeholder="메뉴 설명"
              {...register("description")}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-category">카테고리 (선택)</Label>
            <Input
              id="menu-category"
              placeholder="예: 메인, 음료, 디저트"
              {...register("category")}
            />
          </div>

          <div className="flex items-center justify-between">
            <Label htmlFor="menu-available">판매 가능</Label>
            <Switch
              id="menu-available"
              checked={isAvailable ?? true}
              onCheckedChange={(checked) => setValue("isAvailable", checked)}
            />
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              className="bg-transparent"
            >
              취소
            </Button>
            <Button type="submit" disabled={!isValid || isPending}>
              {isPending
                ? isEdit
                  ? "수정 중..."
                  : "추가 중..."
                : isEdit
                  ? "수정"
                  : "추가"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
