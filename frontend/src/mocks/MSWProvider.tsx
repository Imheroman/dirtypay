'use client';

import { useEffect, useState } from 'react';

interface MSWProviderProps {
  children: React.ReactNode;
}

export function MSWProvider({ children }: MSWProviderProps) {
  const [isMounted, setIsMounted] = useState(false);

  useEffect(() => {
    async function initMSW() {
      if (process.env.NODE_ENV === 'development' && process.env.NEXT_PUBLIC_USE_MSW === 'true') {
        const { worker } = await import('./browser');
        await worker.start({
          onUnhandledRequest: 'bypass',
        });
      }
      setIsMounted(true);
    }

    initMSW();
  }, []);

  if (!isMounted) {
    return null;
  }

  return <>{children}</>;
}
