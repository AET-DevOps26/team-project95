import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

export type QueryMode = 'Natural Language' | 'Filters' | 'Both';

export type FilterState = {
  chairIds: number[];
  degreeTypes: string[];
  researchAreas: string[];
  tags: string[];
};

export const INITIAL_FILTER_STATE: FilterState = {
  chairIds: [],
  degreeTypes: [],
  researchAreas: [],
  tags: [],
};

type SearchStateContextValue = {
  queryMode: QueryMode;
  setQueryMode: (queryMode: QueryMode) => void;
  naturalLanguageQuery: string;
  setNaturalLanguageQuery: (query: string) => void;
  selectedFilters: FilterState;
  setSelectedFilters: React.Dispatch<React.SetStateAction<FilterState>>;
  resetSelectedFilters: () => void;
};

const SearchStateContext = createContext<SearchStateContextValue | null>(null);

export function SearchStateProvider({ children }: { children: ReactNode }) {
  const [queryMode, setQueryMode] = useState<QueryMode>('Both');
  const [naturalLanguageQuery, setNaturalLanguageQuery] = useState('');
  const [selectedFilters, setSelectedFilters] = useState<FilterState>(INITIAL_FILTER_STATE);

  const value = useMemo(
    () => ({
      queryMode,
      setQueryMode,
      naturalLanguageQuery,
      setNaturalLanguageQuery,
      selectedFilters,
      setSelectedFilters,
      resetSelectedFilters: () => setSelectedFilters(INITIAL_FILTER_STATE),
    }),
    [naturalLanguageQuery, queryMode, selectedFilters],
  );

  return <SearchStateContext.Provider value={value}>{children}</SearchStateContext.Provider>;
}

export function useSearchState() {
  const value = useContext(SearchStateContext);

  if (!value) {
    throw new Error('useSearchState must be used inside SearchStateProvider');
  }

  return value;
}
