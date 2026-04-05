'use client';

import { useState, useCallback, useRef, useMemo } from 'react';
import type { Store, StoreType } from '../types';
import { parseStoreDescription } from '../utils';

interface StoreFormValues {
  name: string;
  address: string;
  description: string;
  storeType: StoreType;
  customTypeName: string;
  businessNumber: string;
  phone: string;
  posIntegrationKey: string;
}

function getInitialValues(store?: Store): StoreFormValues {
  if (store) {
    const { customType, cleanDescription } = parseStoreDescription(store.description);
    return {
      name: store.name,
      address: store.address,
      description: cleanDescription ?? '',
      storeType: customType ? 'CUSTOM' : store.storeType,
      customTypeName: customType ?? '',
      businessNumber: store.businessNumber ?? '',
      phone: store.phone ?? '',
      posIntegrationKey: '',
    };
  }

  return {
    name: '',
    address: '',
    description: '',
    storeType: 'DIRECT',
    customTypeName: '',
    businessNumber: '',
    phone: '',
    posIntegrationKey: '',
  };
}

export function useStoreForm(store?: Store) {
  const [values, setValues] = useState<StoreFormValues>(() =>
    getInitialValues(store)
  );
  const initialValuesRef = useRef<StoreFormValues>(getInitialValues(store));

  const isDirty = useMemo(() => {
    const initial = initialValuesRef.current;
    return (Object.keys(initial) as (keyof StoreFormValues)[]).some(
      (key) => values[key] !== initial[key]
    );
  }, [values]);

  const handleChange = useCallback(
    (
      e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
      const { name, value } = e.target;
      setValues((prev) => {
        const next = { ...prev, [name]: value };

        // storeType 변경 시 연관 필드 리셋
        if (name === 'storeType') {
          if (value === 'CUSTOM') {
            next.businessNumber = '';
            next.posIntegrationKey = '';
          } else if (value === 'DIRECT') {
            next.posIntegrationKey = '';
          } else if (value === 'POS_INTEGRATED') {
            // posIntegrationKey 유지
          }
        }

        return next;
      });
    },
    []
  );

  const setFieldValue = useCallback(
    (name: keyof StoreFormValues, value: StoreFormValues[keyof StoreFormValues]) => {
      setValues((prev) => ({ ...prev, [name]: value }));
    },
    []
  );

  const reset = useCallback((nextStore?: Store) => {
    const next = getInitialValues(nextStore);
    setValues(next);
    initialValuesRef.current = next;
  }, []);

  const isCustomType = values.storeType === 'CUSTOM';

  return { values, handleChange, setFieldValue, reset, isCustomType, isDirty };
}
