"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createStoreSchema, type CreateStoreFormData } from "../schemas";
import { useCreateStoreMutation } from "../hooks";

export function CreateStoreForm() {
  const router = useRouter();
  const createMutation = useCreateStoreMutation();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isValid },
  } = useForm<CreateStoreFormData>({
    resolver: zodResolver(createStoreSchema),
    mode: "onBlur",
    reValidateMode: "onChange",
    defaultValues: {
      name: "",
      address: "",
      description: "",
    },
  });

  const onSubmit = (data: CreateStoreFormData) => {
    createMutation.mutate(
      {
        name: data.name,
        address: data.address,
        description: data.description || undefined,
      },
      {
        onSuccess: () => {
          router.push("/stores");
        },
        onError: () => {
          setError("root", { message: "매장 등록에 실패했어요" });
        },
      },
    );
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
      <div className="space-y-2">
        <Label htmlFor="name">매장 이름</Label>
        <Input
          id="name"
          placeholder="매장 이름을 입력해 주세요"
          aria-invalid={!!errors.name}
          aria-describedby={errors.name ? "name-error" : undefined}
          {...register("name")}
        />
        {errors.name && (
          <p id="name-error" className="text-sm text-destructive" role="alert">
            {errors.name.message}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="address">주소</Label>
        <Input
          id="address"
          placeholder="매장 주소를 입력해 주세요"
          aria-invalid={!!errors.address}
          aria-describedby={errors.address ? "address-error" : undefined}
          {...register("address")}
        />
        {errors.address && (
          <p
            id="address-error"
            className="text-sm text-destructive"
            role="alert"
          >
            {errors.address.message}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="description">설명 (선택)</Label>
        <Input
          id="description"
          placeholder="매장에 대해 소개해 주세요"
          aria-invalid={!!errors.description}
          {...register("description")}
        />
        {errors.description && (
          <p className="text-sm text-destructive" role="alert">
            {errors.description.message}
          </p>
        )}
      </div>

      {errors.root && (
        <p className="text-sm text-destructive" role="alert">
          {errors.root.message}
        </p>
      )}

      <Button
        type="submit"
        className="w-full"
        disabled={!isValid || createMutation.isPending}
      >
        {createMutation.isPending ? "등록 중..." : "매장 등록"}
      </Button>
    </form>
  );
}
