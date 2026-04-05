/**
 * Custom store type encoding/decoding.
 *
 * Backend supports 'DIRECT' | 'POS_INTEGRATED' | 'CUSTOM'.
 * Custom type names (e.g. "나만의 가게") are stored as a
 * [type:name] prefix in the description field.
 */

const CUSTOM_TYPE_PREFIX = '[type:';
const CUSTOM_TYPE_SUFFIX = ']';

export function encodeCustomType(
  customTypeName: string,
  description?: string,
): string {
  const prefix = `${CUSTOM_TYPE_PREFIX}${customTypeName}${CUSTOM_TYPE_SUFFIX}`;
  return description ? `${prefix} ${description}` : prefix;
}

export function parseStoreDescription(description: string | null): {
  customType: string | null;
  cleanDescription: string | null;
} {
  if (!description) return { customType: null, cleanDescription: null };

  if (description.startsWith(CUSTOM_TYPE_PREFIX)) {
    const endIndex = description.indexOf(CUSTOM_TYPE_SUFFIX);
    if (endIndex !== -1) {
      const customType = description.slice(CUSTOM_TYPE_PREFIX.length, endIndex);
      const rest = description.slice(endIndex + 1).trim();
      return {
        customType,
        cleanDescription: rest || null,
      };
    }
  }

  return { customType: null, cleanDescription: description };
}

/** Resolve display label for store type */
export function getStoreTypeLabel(
  storeType: string,
  description: string | null,
): string {
  const { customType } = parseStoreDescription(description);
  if (customType) return customType;
  if (storeType === 'CUSTOM') return '나만의 가게';
  return storeType === 'POS_INTEGRATED' ? 'POS 연동' : '직접 운영';
}
