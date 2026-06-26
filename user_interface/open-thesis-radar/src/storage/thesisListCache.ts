import type { components } from '../api';

type ThesisSearchResult = components['schemas']['ThesisSearchResult'];
type ThesisListCache = { totalCount: number; theses: ThesisSearchResult[]; cachedAt: string };

const CACHE_KEY = 'open-thesis-radar:list-theses';
const CACHE_LIMIT = 30;

// Reads the latest cached thesis list preview, ignoring corrupt or outdated browser data.
export function readThesisListCache(): ThesisListCache | null {
  try {
    const cache = JSON.parse(localStorage.getItem(CACHE_KEY) ?? 'null') as ThesisListCache | null;
    return cache && Number.isFinite(cache.totalCount) && Array.isArray(cache.theses) ? cache : null;
  } catch {
    return null;
  }
}

// Stores the real list total and only the first visible thesis preview items.
export function writeThesisListCache(theses: ThesisSearchResult[]) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({ totalCount: theses.length, theses: theses.slice(0, CACHE_LIMIT), cachedAt: new Date().toISOString() }));
  } catch {
    // Ignore unavailable or full browser storage so fresh API results still render normally.
  }
}
