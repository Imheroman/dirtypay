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
import { ConfirmModal } from '@/components/common';
import { useStoreForm } from '../hooks/useStoreForm';
import { encodeCustomType } from '../utils';
import type { Store, StoreType, CreateStoreRequest, UpdateStoreRequest } from '../types';

const CUSTOM_PRESETS = ['나만의 가게', '나만의 집', '비밀 가게'];

export interface StoreFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (
    data: CreateStoreRequest | UpdateStoreRequest
  ) => void | Promise<void>;
  store?: Store;
  isLoading?: boolean;
}

export function StoreForm({
  isOpen,
  onClose,
  onSubmit,
  store,
  isLoading = false,
}: StoreFormProps) {
  const isEditing = !!store;
  const { values, handleChange, setFieldValue, reset, isCustomType, isDirty } =
    useStoreForm(store);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);

  useEffect(() => {
    if (isOpen) {
      reset(store);
    }
  }, [isOpen, store]);

  const handleClose = () => {
    reset();
    onClose();
  };

  const handleAttemptClose = () => {
    if (isDirty) {
      setIsConfirmOpen(true);
    } else {
      handleClose();
    }
  };

  const handleConfirmClose = () => {
    setIsConfirmOpen(false);
    handleClose();
  };

  const handleCancelClose = () => {
    setIsConfirmOpen(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Build description: encode custom type name if CUSTOM
    const description = isCustomType && values.customTypeName.trim()
      ? encodeCustomType(values.customTypeName.trim(), values.description || undefined)
      : values.description || undefined;

    const payload = isEditing
      ? ({
          name: values.name || undefined,
          address: values.address || undefined,
          description,
          ...(values.phone.trim() && { phone: values.phone.trim() }),
        } satisfies UpdateStoreRequest)
      : ({
          name: values.name,
          address: values.address,
          description,
          storeType: values.storeType as StoreType,
          ...(values.businessNumber.trim() && { businessNumber: values.businessNumber.trim() }),
          ...(values.phone.trim() && { phone: values.phone.trim() }),
          ...(values.storeType === 'POS_INTEGRATED' && values.posIntegrationKey.trim() && {
            posIntegrationKey: values.posIntegrationKey.trim(),
          }),
        } satisfies CreateStoreRequest);

    await onSubmit(payload);
  };

  const isSubmitDisabled =
    isLoading ||
    !values.name.trim() ||
    !values.address.trim() ||
    (isCustomType && !values.customTypeName.trim()) ||
    (!isEditing && values.storeType === 'POS_INTEGRATED' && !values.posIntegrationKey.trim());

  return (
    <Dialog open={isOpen} onOpenChange={(open) => { if (!open) handleAttemptClose(); }}>
      <DialogContent
        className="sm:max-w-lg"
        onInteractOutside={(e) => { if (isDirty) { e.preventDefault(); handleAttemptClose(); } }}
        onEscapeKeyDown={(e) => { if (isDirty) { e.preventDefault(); handleAttemptClose(); } }}
      >
        <DialogHeader>
          <DialogTitle>{isEditing ? '매장 수정' : '매장 등록'}</DialogTitle>
          <DialogDescription>
            {isEditing
              ? '매장 정보를 수정해 주세요.'
              : '새 매장 정보를 입력해 주세요.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="store-name">
              매장명 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="store-name"
              name="name"
              placeholder="예: 더티페이 강남점"
              value={values.name}
              onChange={handleChange}
              maxLength={100}
              required
              autoFocus
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="store-address">
              주소 <span className="text-destructive">*</span>
            </Label>
            <Input
              id="store-address"
              name="address"
              placeholder="예: 서울시 강남구 테헤란로 123"
              value={values.address}
              onChange={handleChange}
              maxLength={255}
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="store-description">소개</Label>
            <Input
              id="store-description"
              name="description"
              placeholder="매장에 대한 간략한 소개를 입력해 주세요."
              value={values.description}
              onChange={handleChange}
              maxLength={1000}
            />
          </div>

          {!isEditing && (
            <>
              <div className="space-y-2">
                <Label htmlFor="store-type">
                  매장 유형 <span className="text-destructive">*</span>
                </Label>
                <select
                  id="store-type"
                  name="storeType"
                  value={values.storeType}
                  onChange={handleChange}
                  required
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <option value="DIRECT" className="text-black">직영</option>
                  <option value="POS_INTEGRATED" className="text-black">POS 연동</option>
                  <option value="CUSTOM" className="text-black">커스텀</option>
                </select>
              </div>

              {isCustomType && (
                <div className="space-y-2">
                  <Label htmlFor="store-custom-type">
                    커스텀 유형 이름 <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="store-custom-type"
                    name="customTypeName"
                    placeholder="예: 나만의 가게"
                    value={values.customTypeName}
                    onChange={handleChange}
                    maxLength={30}
                    required
                  />
                  <div className="flex flex-wrap gap-1.5">
                    {CUSTOM_PRESETS.map((preset) => (
                      <button
                        key={preset}
                        type="button"
                        onClick={() =>
                          handleChange({
                            target: { name: 'customTypeName', value: preset },
                          } as React.ChangeEvent<HTMLInputElement>)
                        }
                        className={`rounded-full border px-2.5 py-0.5 text-xs transition-colors ${
                          values.customTypeName === preset
                            ? 'border-primary bg-primary/10 text-primary'
                            : 'border-border text-muted-foreground hover:border-primary/50'
                        }`}
                      >
                        {preset}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {values.storeType === 'POS_INTEGRATED' && (
                <div className="space-y-2">
                  <Label htmlFor="store-pos-key">
                    POS 연동 키 <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="store-pos-key"
                    name="posIntegrationKey"
                    placeholder="POS 시스템에서 발급받은 연동 키를 입력해 주세요."
                    value={values.posIntegrationKey}
                    onChange={handleChange}
                    maxLength={255}
                    required
                  />
                </div>
              )}

              {!isCustomType && (
                <div className="space-y-2">
                  <Label htmlFor="store-business-number">사업자 등록번호</Label>
                  <Input
                    id="store-business-number"
                    name="businessNumber"
                    placeholder="예: 123-45-67890"
                    value={values.businessNumber}
                    onChange={handleChange}
                    maxLength={20}
                  />
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="store-phone">전화번호</Label>
                <Input
                  id="store-phone"
                  name="phone"
                  placeholder="예: 02-1234-5678"
                  value={values.phone}
                  onChange={handleChange}
                  maxLength={20}
                />
              </div>

            </>
          )}

          {isEditing && (
            <div className="space-y-2">
              <Label htmlFor="store-phone-edit">전화번호</Label>
              <Input
                id="store-phone-edit"
                name="phone"
                placeholder="예: 02-1234-5678"
                value={values.phone}
                onChange={handleChange}
                maxLength={20}
              />
            </div>
          )}

          {/* 수정 모드에서도 커스텀 유형 변경 가능 */}
          {isEditing && (
            <div className="space-y-2">
              <Label htmlFor="store-type-edit">매장 유형</Label>
              <select
                id="store-type-edit"
                name="storeType"
                value={values.storeType}
                onChange={handleChange}
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
              >
                <option value="DIRECT" className="text-black">직영</option>
                <option value="POS_INTEGRATED" className="text-black">POS 연동</option>
                <option value="CUSTOM" className="text-black">커스텀</option>
              </select>

              {isCustomType && (
                <div className="space-y-2 mt-2">
                  <Label htmlFor="store-custom-type-edit">
                    커스텀 유형 이름 <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="store-custom-type-edit"
                    name="customTypeName"
                    placeholder="예: 나만의 가게"
                    value={values.customTypeName}
                    onChange={handleChange}
                    maxLength={30}
                    required
                  />
                  <div className="flex flex-wrap gap-1.5">
                    {CUSTOM_PRESETS.map((preset) => (
                      <button
                        key={preset}
                        type="button"
                        onClick={() =>
                          handleChange({
                            target: { name: 'customTypeName', value: preset },
                          } as React.ChangeEvent<HTMLInputElement>)
                        }
                        className={`rounded-full border px-2.5 py-0.5 text-xs transition-colors ${
                          values.customTypeName === preset
                            ? 'border-primary bg-primary/10 text-primary'
                            : 'border-border text-muted-foreground hover:border-primary/50'
                        }`}
                      >
                        {preset}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          <DialogFooter className="gap-2 pt-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={handleAttemptClose}
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

      <ConfirmModal
        isOpen={isConfirmOpen}
        onClose={handleCancelClose}
        onConfirm={handleConfirmClose}
        title="작성 중인 내용이 있어요"
        description="지금 나가면 입력한 내용이 사라져요. 정말 나갈까요?"
        confirmText="나가기"
        cancelText="계속 작성"
      />
    </Dialog>
  );
}
