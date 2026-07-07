/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

export type FilterState = {
  chairIds: number[];
  degreeTypes: string[];
  researchAreas: string[];
};

export const INITIAL_FILTER_STATE: FilterState = {
  chairIds: [],
  degreeTypes: [],
  researchAreas: [],
};

type SearchStateContextValue = {
  naturalLanguageQuery: string;
  setNaturalLanguageQuery: (query: string) => void;
  selectedFilters: FilterState;
  setSelectedFilters: React.Dispatch<React.SetStateAction<FilterState>>;
  resetSelectedFilters: () => void;
};

const SearchStateContext = createContext<SearchStateContextValue | null>(null);

export function SearchStateProvider({ children }: { children: ReactNode }) {
  const [naturalLanguageQuery, setNaturalLanguageQuery] = useState('');
  const [selectedFilters, setSelectedFilters] = useState<FilterState>(INITIAL_FILTER_STATE);

  const value = useMemo(
    () => ({
      naturalLanguageQuery,
      setNaturalLanguageQuery,
      selectedFilters,
      setSelectedFilters,
      resetSelectedFilters: () => setSelectedFilters(INITIAL_FILTER_STATE),
    }),
    [naturalLanguageQuery, selectedFilters],
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
