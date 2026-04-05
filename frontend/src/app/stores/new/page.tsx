'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { ChevronLeftIcon } from '@/components/common/Icons';
import { useCreateStoreMutation, StoreForm, type CreateStoreRequest, type UpdateStoreRequest } from '@/features/store';

export default function CreateStorePage() {
  const router = useRouter();
  const createStoreMutation = useCreateStoreMutation();

  const handleSubmit = (request: CreateStoreRequest | UpdateStoreRequest) => {
    if (!('storeType' in request)) return;  // UpdateStoreRequest guard
    createStoreMutation.mutate(request, {
      onSuccess: () => {
        router.push('/stores');
      },
    });
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link href="/stores" className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors">
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">새 가게 등록</h1>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        <StoreForm
          isOpen={true}
          onClose={() => router.push('/stores')}
          onSubmit={handleSubmit}
          isLoading={createStoreMutation.isPending}
        />
      </main>
    </div>
  );
}
