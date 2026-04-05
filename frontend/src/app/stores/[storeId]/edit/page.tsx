'use client';

import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import { ChevronLeftIcon } from '@/components/common/Icons';
import {
  useStoreQuery,
  useUpdateStoreMutation,
  StoreForm,
  type UpdateStoreRequest,
} from '@/features/store';
import { LoadingSpinner } from '@/components/common';

export default function EditStorePage() {
  const router = useRouter();
  const params = useParams();
  const storeId = params?.storeId as string;

  const { data: store, isLoading } = useStoreQuery(storeId);
  const updateStoreMutation = useUpdateStoreMutation();

  const handleSubmit = (request: UpdateStoreRequest) => {
    updateStoreMutation.mutate(
      { storeId: Number(storeId), request },
      {
        onSuccess: () => {
          router.push(`/stores/${storeId}`);
        },
      }
    );
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!store) {
    return (
      <div className="min-h-screen bg-background">
        <header className="sticky top-0 z-50 bg-background/95 backdrop-blur border-b border-border">
          <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
            <Link href={`/stores/${storeId}`} className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors">
              <ChevronLeftIcon className="w-5 h-5 text-foreground" />
            </Link>
            <h1 className="text-lg font-semibold text-foreground flex-1">가게 수정</h1>
          </div>
        </header>
        <main className="container mx-auto px-4 py-6 max-w-lg">
          <div className="p-6 text-center text-muted-foreground">가게 정보를 불러오지 못했어요.</div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* 헤더 */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur border-b border-border">
        <div className="container mx-auto px-4 h-14 flex items-center gap-3 max-w-lg">
          <Link href={`/stores/${storeId}`} className="p-1 -ml-1 rounded-lg hover:bg-accent transition-colors">
            <ChevronLeftIcon className="w-5 h-5 text-foreground" />
          </Link>
          <h1 className="text-lg font-semibold text-foreground flex-1">가게 수정</h1>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6 max-w-lg">
        <StoreForm
          isOpen={true}
          onClose={() => router.push(`/stores/${storeId}`)}
          onSubmit={handleSubmit}
          store={store}
          isLoading={updateStoreMutation.isPending}
        />
      </main>
    </div>
  );
}
