import { z } from 'zod';

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, '이메일을 입력해 주세요')
    .email('올바른 이메일을 입력해 주세요'),
  password: z
    .string()
    .min(1, '비밀번호를 입력해 주세요')
    .max(100, '비밀번호는 100자 이하여야 해요'),
});

export const signupSchema = z
  .object({
    email: z
      .string()
      .min(1, '이메일을 입력해 주세요')
      .email('올바른 이메일을 입력해 주세요'),
    name: z
      .string()
      .min(1, '이름을 입력해 주세요'),
    password: z
      .string()
      .min(8, '비밀번호는 8자 이상이에요')
      .max(100, '비밀번호는 100자 이하여야 해요')
      .regex(
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/,
        '대문자, 소문자, 숫자, 특수문자(@$!%*?&)를 각각 1개 이상 포함해 주세요'
      ),
    confirmPassword: z
      .string()
      .min(1, '비밀번호를 다시 입력해 주세요'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: '비밀번호가 일치하지 않아요',
    path: ['confirmPassword'],
  });

export type LoginFormData = z.infer<typeof loginSchema>;
export type SignupFormData = z.infer<typeof signupSchema>;
