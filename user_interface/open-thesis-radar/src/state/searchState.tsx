/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import type { components } from '../api';

type ThesisSearchResult = components['schemas']['ThesisSearchResult'];

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
  currentPage: number;
  setCurrentPage: React.Dispatch<React.SetStateAction<number>>;
  serverSearchResults: ThesisSearchResult[] | null;
  setServerSearchResults: React.Dispatch<React.SetStateAction<ThesisSearchResult[] | null>>;
};

const SearchStateContext = createContext<SearchStateContextValue | null>(null);

export function SearchStateProvider({ children }: { children: ReactNode }) {
  const [naturalLanguageQuery, setNaturalLanguageQuery] = useState('');
  const [selectedFilters, setSelectedFilters] = useState<FilterState>(INITIAL_FILTER_STATE);
  const [currentPage, setCurrentPage] = useState(1);
  const [serverSearchResults, setServerSearchResults] = useState<ThesisSearchResult[] | null>(null);

  const value = useMemo(
    () => ({
      naturalLanguageQuery,
      setNaturalLanguageQuery,
      selectedFilters,
      setSelectedFilters,
      resetSelectedFilters: () => setSelectedFilters(INITIAL_FILTER_STATE),
      currentPage,
      setCurrentPage,
      serverSearchResults,
      setServerSearchResults,
    }),
    [currentPage, naturalLanguageQuery, selectedFilters, serverSearchResults],
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
