import { z } from "zod";

export const createStoreSchema = z.object({
  name: z
    .string()
    .min(1, "매장 이름을 입력해 주세요")
    .max(50, "매장 이름은 50자 이하여야 해요"),
  address: z
    .string()
    .min(1, "주소를 입력해 주세요")
    .max(200, "주소는 200자 이하여야 해요"),
  description: z
    .string()
    .max(500, "설명은 500자 이하여야 해요")
    .optional()
    .or(z.literal("")),
});

export const updateStoreSchema = z.object({
  name: z
    .string()
    .min(1, "매장 이름을 입력해 주세요")
    .max(50, "매장 이름은 50자 이하여야 해요")
    .optional(),
  address: z
    .string()
    .min(1, "주소를 입력해 주세요")
    .max(200, "주소는 200자 이하여야 해요")
    .optional(),
  description: z
    .string()
    .max(500, "설명은 500자 이하여야 해요")
    .optional()
    .or(z.literal("")),
});

export const createMenuSchema = z.object({
  name: z
    .string()
    .min(1, "메뉴 이름을 입력해 주세요")
    .max(50, "메뉴 이름은 50자 이하여야 해요"),
  price: z
    .number({ error: "가격을 입력해 주세요" })
    .min(0, "가격은 0원 이상이어야 해요")
    .max(10_000_000, "가격은 1,000만원 이하여야 해요"),
  description: z
    .string()
    .max(200, "설명은 200자 이하여야 해요")
    .optional()
    .or(z.literal("")),
  category: z
    .string()
    .max(30, "카테고리는 30자 이하여야 해요")
    .optional()
    .or(z.literal("")),
  imageUrl: z.string().optional().or(z.literal("")),
  isAvailable: z.boolean().optional(),
});

export const becomeSellerSchema = z.object({
  businessName: z
    .string()
    .max(100, "사업명은 100자 이하여야 해요")
    .optional()
    .or(z.literal("")),
  businessRegistration: z
    .string()
    .max(20, "사업자등록번호는 20자 이하여야 해요")
    .optional()
    .or(z.literal("")),
  phone: z
    .string()
    .max(20, "전화번호는 20자 이하여야 해요")
    .optional()
    .or(z.literal("")),
  email: z
    .string()
    .email("올바른 이메일을 입력해 주세요")
    .optional()
    .or(z.literal("")),
});

export type CreateStoreFormData = z.infer<typeof createStoreSchema>;
export type UpdateStoreFormData = z.infer<typeof updateStoreSchema>;
export type CreateMenuFormData = z.infer<typeof createMenuSchema>;
export type BecomeSellerFormData = z.infer<typeof becomeSellerSchema>;
