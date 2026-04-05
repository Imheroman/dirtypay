'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuthContext } from '@/components/providers/auth-provider';
import { loginSchema, type LoginFormData } from '../schemas';

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuthContext();

  const [isPending, setIsPending] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    reValidateMode: 'onChange',
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsPending(true);

    const result = await login({ email: data.email, password: data.password });

    if (result.success) {
      const callbackUrl = searchParams.get('callbackUrl') || '/';
      router.push(callbackUrl);
    } else {
      setError('root', { message: result.error || '로그인에 실패했어요' });
      setIsPending(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
      <div className="space-y-2">
        <Label htmlFor="email">이메일</Label>
        <Input
          id="email"
          type="email"
          placeholder="example@email.com"
          autoComplete="email"
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? 'email-error' : undefined}
          {...register('email')}
        />
        {errors.email && (
          <p id="email-error" className="text-sm text-destructive" role="alert">
            {errors.email.message}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="password">비밀번호</Label>
        <Input
          id="password"
          type="password"
          placeholder="비밀번호를 입력하세요"
          autoComplete="current-password"
          aria-invalid={!!errors.password}
          aria-describedby={errors.password ? 'password-error' : undefined}
          {...register('password')}
        />
        {errors.password && (
          <p id="password-error" className="text-sm text-destructive" role="alert">
            {errors.password.message}
          </p>
        )}
      </div>

      {errors.root && (
        <p className="text-sm text-destructive" role="alert">
          {errors.root.message}
        </p>
      )}

      <Button type="submit" className="w-full" disabled={isPending}>
        {isPending ? '로그인 중...' : '로그인'}
      </Button>

      <p className="text-center text-sm text-muted-foreground">
        아직 계정이 없으신가요?{' '}
        <Link href="/signup" className="text-primary hover:underline">
          회원가입
        </Link>
      </p>
    </form>
  );
}
