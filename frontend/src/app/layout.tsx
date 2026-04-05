import type { Metadata, Viewport } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.css';
import { Providers } from '@/providers';
import { Header, Footer, ErrorBoundary } from '@/components/common';

const geistSans = Geist({
  variable: '--font-geist-sans',
  subsets: ['latin'],
});

const geistMono = Geist_Mono({
  variable: '--font-geist-mono',
  subsets: ['latin'],
});

export const metadata: Metadata = {
  title: 'Dirty Pay - 정밀 N/1 정산 서비스',
  description: '복잡한 정산을 깔끔하게. 트리 구조 조직 관리와 라운드 기반 정밀 정산',
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased min-h-screen flex flex-col`}
      >
        <Providers>
          <Header />
          <main className="flex-1">
            <ErrorBoundary>{children}</ErrorBoundary>
          </main>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
