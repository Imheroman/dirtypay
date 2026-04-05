"use client";

import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { ChevronLeftIcon } from "@/components/common/Icons";
import { useAuthContext } from "@/components/providers/auth-provider";
import { CreateStoreForm } from "@/features/seller";

export default function CreateStorePage() {
  const { isAuthenticated, isLoading } = useAuthContext();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-muted-foreground">로그인이 필요해요.</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80 border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link
            href="/stores"
            className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors"
          >
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">
            매장 등록
          </h1>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        <Card>
          <CardContent className="p-6">
            <CreateStoreForm />
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
