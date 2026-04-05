'use client';

import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import type {
  StoreMenu,
  CreateStoreMenuRequest,
  UpdateStoreMenuRequest,
} from '../types';

export interface StoreMenuFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (
    data: CreateStoreMenuRequest | UpdateStoreMenuRequest
  ) => void | Promise<void>;
  menu?: StoreMenu;
  isLoading?: boolean;
}

interface MenuFormValues {
  name: string;
  description: string;
  price: string;
  category: string;
  imageUrl: string;
  available: boolean;
  sortOrder: string;
}

function getInitialValues(menu?: StoreMenu): MenuFormValues {
  return {
    name: menu?.name ?? '',
    description: menu?.description ?? '',
    price: menu ? String(menu.price) : '',
    category: menu?.category ?? '',
    imageUrl: menu?.imageUrl ?? '',
    available: menu?.available ?? true,
    sortOrder: menu ? String(menu.sortOrder) : '',
  };
}

export function StoreMenuForm({
  isOpen,
  onClose,
  onSubmit,
  menu,
  isLoading = false,
}: StoreMenuFormProps) {
  const isEditing = !!menu;
  const [values, setValues] = useState<MenuFormValues>(() =>
    getInitialValues(menu)
  );

  useEffect(() => {
    if (isOpen) {
      setValues(getInitialValues(menu));
    }
  }, [isOpen, menu]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setValues((prev) => ({ ...prev, [name]: value }));
  };

  const handleClose = () => {
    setValues(getInitialValues());
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const priceNum = Number(values.price);
    if (!values.name.trim() || Number.isNaN(priceNum) || priceNum <= 0) return;

    const sortOrderNum = values.sortOrder ? Number(values.sortOrder) : undefined;

    const payload = isEditing
      ? ({
          name: values.name.trim() || undefined,
          description: values.description.trim() || undefined,
          price: priceNum,
          category: values.category.trim() || undefined,
          imageUrl: values.imageUrl.trim() || undefined,
          sortOrder: sortOrderNum,
        } satisfies UpdateStoreMenuRequest)
      : ({
          name: values.name.trim(),
          description: values.description.trim() || undefined,
          price: priceNum,
          category: values.category.trim() || undefined,
          imageUrl: values.imageUrl.trim() || undefined,
          available: values.available,
          sortOrder: sortOrderNum,
        } satisfies CreateStoreMenuRequest);

    await onSubmit(payload);
  };

  const priceNum = Number(values.price);
  const isSubmitDisabled =
    isLoading ||
    !values.name.trim() ||
    !values.price ||
    Number.isNaN(priceNum) ||
    priceNum <= 0;

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditing ? '메뉴 수정' : '메뉴 등록'}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? '메뉴 정보를 수정해 주세요.'
              : '새 메뉴 정보를 입력해 주세요.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="menu-name">
              메뉴명 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="menu-name"
              name="name"
              placeholder="예: 삼겹살 1인분"
              value={values.name}
              onChange={handleChange}
              maxLength={100}
              required
              autoFocus
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-description">설명</Label>
            <Input
              id="menu-description"
              name="description"
              placeholder="메뉴에 대한 간략한 설명"
              value={values.description}
              onChange={handleChange}
              maxLength={500}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-price">
              가격 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="menu-price"
              name="price"
              type="number"
              placeholder="15000"
              value={values.price}
              onChange={handleChange}
              min={1}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-category">카테고리</Label>
            <Input
              id="menu-category"
              name="category"
              placeholder="예: 육류, 해산물"
              value={values.category}
              onChange={handleChange}
              maxLength={50}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-image-url">이미지 URL</Label>
            <Input
              id="menu-image-url"
              name="imageUrl"
              type="url"
              placeholder="https://example.com/image.jpg"
              value={values.imageUrl}
              onChange={handleChange}
              maxLength={500}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="menu-sort-order">노출 순서</Label>
            <Input
              id="menu-sort-order"
              name="sortOrder"
              type="number"
              placeholder="0"
              value={values.sortOrder}
              onChange={handleChange}
              min={0}
            />
          </div>

          <div className="flex items-center gap-2">
            <Checkbox
              id="menu-available"
              checked={values.available}
              onCheckedChange={(checked) =>
                setValues((prev) => ({
                  ...prev,
                  available: checked === true,
                }))
              }
              aria-label="판매 가능 여부"
            />
            <Label
              htmlFor="menu-available"
              className="cursor-pointer font-normal"
            >
              판매 가능
            </Label>
          </div>

          <DialogFooter className="gap-2 pt-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isLoading}
              className="bg-transparent"
            >
              취소
            </Button>
            <Button type="submit" disabled={isSubmitDisabled}>
              {isLoading ? '처리 중...' : isEditing ? '수정' : '등록'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
