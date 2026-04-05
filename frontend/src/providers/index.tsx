'use client';

import { ThemeProvider } from 'next-themes';
import { QueryProvider } from './QueryProvider';
import { MSWProvider } from '@/mocks/MSWProvider';
import { AuthProvider } from '@/components/providers/auth-provider';
import { Toaster } from '@/components/ui/sonner';

interface ProvidersProps {
  children: React.ReactNode;
}

export function Providers({ children }: ProvidersProps) {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      <MSWProvider>
        <QueryProvider>
          <AuthProvider>
            {children}
            <Toaster />
          </AuthProvider>
        </QueryProvider>
      </MSWProvider>
    </ThemeProvider>
  );
}
