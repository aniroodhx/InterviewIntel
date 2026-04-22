type CacheEntry = {
  data: any;
  timestamp: number;
};

const CACHE_TTL = 1000 * 60 * 60; 

const memoryCache = new Map<string, CacheEntry>();

export function getFromMemory(key: string) {
  const entry = memoryCache.get(key);

  if (!entry) return null;

  const isExpired = Date.now() - entry.timestamp > CACHE_TTL;

  if (isExpired) {
    memoryCache.delete(key);
    return null;
  }

  return entry.data;
}

export function setToMemory(key: string, data: any) {
  memoryCache.set(key, {
    data,
    timestamp: Date.now(),
  });
}