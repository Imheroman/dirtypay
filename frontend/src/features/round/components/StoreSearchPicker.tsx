'use client';

import { useState, useRef, useEffect, useMemo } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { SearchIcon, MapPinIcon, XIcon } from '@/components/common/Icons';
import { useStoresQuery } from '@/features/store/hooks';

export interface PickerStore {
  id: number;
  name: string;
  address: string;
}

interface StoreSearchPickerProps {
  selectedStore: PickerStore | null;
  onSelectStore: (store: PickerStore) => void;
  onClearStore: () => void;
}

export function StoreSearchPicker({
  selectedStore,
  onSelectStore,
  onClearStore,
}: StoreSearchPickerProps) {
  const [search, setSearch] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // 내 가게 조회 (scope=my → CUSTOM 포함)
  const { data: myStoresData } = useStoresQuery({ page: 0, size: 100, scope: 'my' });
  const myStores = myStoresData?.content ?? [];

  // 전체 가게 조회
  const { data: allStoresData } = useStoresQuery({ page: 0, size: 100 });
  const allStores = allStoresData?.content ?? [];

  // 검색 필터링 + 내 가게 우선 표시
  const { mySection, otherSection } = useMemo(() => {
    const myStoreIds = new Set(myStores.map((s) => s.id));
    const query = search.toLowerCase();

    const filterFn = (store: { name: string; address: string }) =>
      !query ||
      store.name.toLowerCase().includes(query) ||
      store.address.toLowerCase().includes(query);

    const filteredMy = myStores.filter(filterFn).map((s) => ({
      id: s.id,
      name: s.name,
      address: s.address,
    }));

    const filteredOther = allStores
      .filter((s) => !myStoreIds.has(s.id) && filterFn(s))
      .map((s) => ({
        id: s.id,
        name: s.name,
        address: s.address,
      }));

    return { mySection: filteredMy, otherSection: filteredOther };
  }, [myStores, allStores, search]);

  // 외부 클릭 시 드롭다운 닫기
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (store: PickerStore) => {
    onSelectStore(store);
    setSearch('');
    setIsOpen(false);
  };

  if (selectedStore) {
    return (
      <div className="flex items-center gap-2 p-2 bg-primary/5 rounded-lg border border-primary/20">
        <MapPinIcon className="w-4 h-4 text-primary shrink-0" />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-foreground truncate">{selectedStore.name}</p>
          <p className="text-xs text-muted-foreground truncate">{selectedStore.address}</p>
        </div>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="h-6 w-6 p-0"
          onClick={onClearStore}
        >
          <XIcon className="w-3 h-3" />
        </Button>
      </div>
    );
  }

  const hasResults = mySection.length > 0 || otherSection.length > 0;

  return (
    <div ref={containerRef} className="relative">
      <div className="relative">
        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <Input
          placeholder="가게 이름으로 검색..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(true)}
          className="pl-10"
        />
      </div>

      {isOpen && (
        <Card className="absolute z-10 w-full mt-1 shadow-lg max-h-60 overflow-y-auto">
          <CardContent className="p-2">
            {hasResults ? (
              <div className="space-y-1">
                {mySection.length > 0 && (
                  <>
                    <p className="text-xs font-semibold text-muted-foreground px-2 pt-1">내 가게</p>
                    {mySection.map((store) => (
                      <button
                        key={store.id}
                        type="button"
                        className="w-full text-left p-2 rounded-lg hover:bg-accent transition-colors"
                        onClick={() => handleSelect(store)}
                      >
                        <p className="font-medium text-foreground text-sm">{store.name}</p>
                        <p className="text-xs text-muted-foreground">{store.address}</p>
                      </button>
                    ))}
                  </>
                )}
                {otherSection.length > 0 && (
                  <>
                    <p className="text-xs font-semibold text-muted-foreground px-2 pt-1">전체 가게</p>
                    {otherSection.map((store) => (
                      <button
                        key={store.id}
                        type="button"
                        className="w-full text-left p-2 rounded-lg hover:bg-accent transition-colors"
                        onClick={() => handleSelect(store)}
                      >
                        <p className="font-medium text-foreground text-sm">{store.name}</p>
                        <p className="text-xs text-muted-foreground">{store.address}</p>
                      </button>
                    ))}
                  </>
                )}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground p-2 text-center">
                검색 결과가 없어요
              </p>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
