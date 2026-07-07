import { useEffect, useMemo, useRef, useState } from 'react';
import styles from '../style/HomePage.module.css';
import scrollIcon from '/assets/icons/chevrons-down.svg';
import FilterDropdown from '../components/FilterDropdown';
import { getAvailableFilters } from '../api/filters';
import { listTheses, searchTheses } from '../api/theses';
import { readThesisListCache, writeThesisListCache } from '../storage/thesisListCache';
import type { components } from '../api';
// import { MOCK_FILTERS } from '../mocks/theses';
import { useSearchState } from '../state/searchState';
import { Link } from 'react-router-dom';

type ThesisSearchResult = components['schemas']['ThesisSearchResult'];

const THESES_PER_PAGE = 21;


function thesisMatchesSelectedFilters(thesis: ThesisSearchResult, selectedFilters: components['schemas']['ThesisSearchFilters']) {
  const matchesChair = !selectedFilters.chairIds?.length || selectedFilters.chairIds.includes(thesis.chairId);
  const matchesDegree =
    !selectedFilters.degreeTypes?.length || Boolean(thesis.degreeType && selectedFilters.degreeTypes.includes(thesis.degreeType));
  const matchesResearchArea =
    !selectedFilters.researchAreas?.length ||
    Boolean(thesis.researchArea && selectedFilters.researchAreas.includes(thesis.researchArea));

  return matchesChair && matchesDegree && matchesResearchArea;
}

export default function HomePage() {
  const cachedThesisList = useMemo(() => readThesisListCache(), []);
  const [filters, setFilters] = useState<components['schemas']['AvailableFiltersResponse'] | null>(null);
  const [allTheses, setAllTheses] = useState<ThesisSearchResult[]>(cachedThesisList?.theses ?? []);
  const [thesisTotalCount, setThesisTotalCount] = useState(cachedThesisList?.totalCount ?? 0);
  const [serverSearchResults, setServerSearchResults] = useState<ThesisSearchResult[] | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [isLoadingTheses, setIsLoadingTheses] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [resultsError, setResultsError] = useState<string | null>(null);
  const searchSectionRef = useRef<HTMLElement | null>(null);
  const resultsHeaderRef = useRef<HTMLDivElement | null>(null);
  const {
    naturalLanguageQuery,
    setNaturalLanguageQuery,
    selectedFilters,
    setSelectedFilters,
    resetSelectedFilters,
  } = useSearchState();
  const normalizedNaturalLanguageQuery = naturalLanguageQuery.trim();
  const selectedApiFilters = useMemo(
    () => ({
      chairIds: selectedFilters.chairIds,
      degreeTypes: selectedFilters.degreeTypes,
      researchAreas: selectedFilters.researchAreas,
      status: 'OPEN',
    }),
    [selectedFilters],
  );
  const shouldUseServerSearch = normalizedNaturalLanguageQuery.length > 0;
  const clientFilteredTheses = useMemo(
    () => allTheses.filter((thesis) => thesisMatchesSelectedFilters(thesis, selectedApiFilters)),
    [allTheses, selectedApiFilters],
  );
  const displayedTheses = serverSearchResults ?? clientFilteredTheses;
  const totalPages = Math.max(1, Math.ceil(displayedTheses.length / THESES_PER_PAGE));
  const visiblePage = Math.min(currentPage, totalPages);
  const pageStartIndex = (visiblePage - 1) * THESES_PER_PAGE;
  const paginatedTheses = displayedTheses.slice(pageStartIndex, pageStartIndex + THESES_PER_PAGE);

  const scrollToSearch = () => {
    searchSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  function changePage(nextPage: number) {
    setCurrentPage(nextPage);

    window.requestAnimationFrame(() => {
      resultsHeaderRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }

  useEffect(() => {
    setCurrentPage(1);
  }, [naturalLanguageQuery, selectedFilters, serverSearchResults]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  useEffect(() => {
    async function loadFilters() {
      try {
        const response = await getAvailableFilters();
        setFilters(response);
      } catch (error) {
        console.error('Failed to load available filters', error);
      }
    }

    async function loadTheses() {
      setIsLoadingTheses(true);
      setResultsError(null);

      try {
        const response = await listTheses();
        setAllTheses(response);
        setThesisTotalCount(response.length);
        writeThesisListCache(response);
      } catch (error) {
        console.error('Failed to load theses', error);
        setResultsError(error instanceof Error ? error.message : 'Failed to load thesis proposals.');
      } finally {
        setIsLoadingTheses(false);
      }
    }

    void loadFilters();
    void loadTheses();
  }, []);

  async function handleSearchSubmit() {
    if (!shouldUseServerSearch) {
      setServerSearchResults(null);
      setResultsError(null);
      return;
    }

    setIsSearching(true);
    setResultsError(null);

    try {
      const response = await searchTheses({
        naturalLanguageQuery: normalizedNaturalLanguageQuery,
        filters: selectedApiFilters,
        page: 0,
        size: 50,
      });
      setServerSearchResults(response.items);
    } catch (error) {
      console.error('Failed to search theses', error);
      setServerSearchResults([]);
      setResultsError(error instanceof Error ? error.message : 'Failed to search thesis proposals.');
    } finally {
      setIsSearching(false);
    }
  }

  const chairOptions = useMemo(
    () =>
      (filters?.chairs ?? []).map((chair) => ({
        value: String(chair.id),
        label: chair.name,
      })),
    [filters?.chairs],
  );

  const degreeTypeOptions = useMemo(
    () => (filters?.degreeTypes ?? []).map((degreeType) => ({ value: degreeType, label: degreeType })),
    [filters?.degreeTypes],
  );

  const researchAreaOptions = useMemo(
    () =>
      (filters?.researchAreas ?? []).map((researchArea) => ({
        value: researchArea,
        label: researchArea,
      })),
    [filters?.researchAreas],
  );

  return (
    <main className={styles.page}>
      <header className={styles.topBar}>
        <div className={styles.topBarInner}>
          <div className={styles.logo}>Open Thesis Radar</div>
          <nav className={styles.topNav} aria-label="Home page navigation">
            <button className={styles.navButton} type="button" onClick={scrollToSearch}>
              Search
            </button>
          </nav>
        </div>
      </header>

      <section className={styles.hero}>
        <div className={styles.heroGrid}>
          <div className={styles.heroCopy}>
            <p className={styles.heroEyebrow}>TUM thesis discovery</p>
            <h1 className={styles.heroTitle}>Search open theses by fit.</h1>
            <p className={styles.heroSubtitle}>
              Compare thesis offers across chairs, degree types, and research areas without losing your filter context while
              you inspect a topic.
            </p>
            <div className={styles.heroActions}>
              <button className={`${styles.primaryAction} ${styles.clickableButton}`} type="button" onClick={scrollToSearch}>
                Search theses
              </button>
            </div>
          </div>

          <div className={styles.heroRadar} aria-hidden="true">
            <div className={styles.radarPulse}></div>
            <span className={`${styles.radarPoint} ${styles.radarPointOne}`}></span>
            <span className={`${styles.radarPoint} ${styles.radarPointTwo}`}></span>
            <span className={`${styles.radarPoint} ${styles.radarPointThree}`}></span>
            <span className={`${styles.radarPoint} ${styles.radarPointFour}`}></span>
            <span className={`${styles.radarPoint} ${styles.radarPointFive}`}></span>
          </div>
        </div>
        <div className={styles.scrollHint}>
          <button className={styles.scrollHintButton} type="button" onClick={scrollToSearch} aria-label="Go to search">
            <img src={scrollIcon} alt="" className={styles.scrollHintIcon} />
          </button>
        </div>
      </section>

      <section className={styles.searchSection} ref={searchSectionRef}>
        <h2 className={styles.searchTitle}>Search Open Theses</h2>
        <p className={styles.searchSubtitle}>
          Describe what you&apos;re looking for or use filters to find the right opportunities.
        </p>

        <form
          className={`${styles.searchBarBlock} ${styles.animatedSection} ${styles.isVisible}`}
          onSubmit={(event) => {
            event.preventDefault();
            void handleSearchSubmit();
          }}
        >
          <input
            className={styles.searchInput}
            onChange={(event) => setNaturalLanguageQuery(event.target.value)}
            placeholder="Describe what you are looking for..."
            type="text"
            value={naturalLanguageQuery}
          />
          <button className={`${styles.searchButton} ${styles.clickableButton}`} type="submit">
            Search
          </button>
        </form>

        <div className={`${styles.filtersCard} ${styles.animatedSection} ${styles.isVisible}`}>
          <div className={styles.filtersHeader}>
            <h3 className={styles.filtersTitle}>Filters</h3>
            <button
              className={`${styles.resetLink} ${styles.clickableButton}`}
              type="button"
              onClick={() => {resetSelectedFilters(); setServerSearchResults(null)}}
            >
              Reset all
            </button>
          </div>
          <div className={styles.filtersGrid}>
            <FilterDropdown
              label="Degree Type"
              values={selectedFilters.degreeTypes}
              options={degreeTypeOptions}
              onChange={(values) => setSelectedFilters((prev) => ({ ...prev, degreeTypes: values }))}
            />
            <FilterDropdown
              label="Research Area"
              values={selectedFilters.researchAreas}
              options={researchAreaOptions}
              onChange={(values) => setSelectedFilters((prev) => ({ ...prev, researchAreas: values }))}
            />
            <FilterDropdown
              label="Chair"
              values={selectedFilters.chairIds.map(String)}
              options={chairOptions}
              onChange={(values) =>
                setSelectedFilters((prev) => ({
                  ...prev,
                  chairIds: values.map((value) => Number(value)),
                }))
              }
            />
          </div>
        </div>

        <div className={styles.resultsHeader} ref={resultsHeaderRef}>
          <div>
            <h3 className={styles.resultsTitle}>Thesis proposals</h3>
            <p className={styles.resultsMeta}>
              {isSearching
                ? 'Searching thesis service…'
                : serverSearchResults
                  ? `${displayedTheses.length} thesis proposals shown`
                  : isLoadingTheses && thesisTotalCount === 0
                    ? 'Loading thesis proposals…'
                    : `${thesisTotalCount} thesis proposals available${isLoadingTheses ? ' · refreshing…' : ''}`}
            </p>
          </div>
        </div>

        {resultsError && <p className={styles.resultsError}>{resultsError}</p>}

        {displayedTheses.length > 0 ? (
          <>
            <div className={styles.resultsGrid}>
              {paginatedTheses.map((thesis) => (
                <Link className={styles.resultCard} to={`/thesis/${thesis.id}`} key={thesis.id}>
                  <div className={styles.resultTopline}>
                    <span>{thesis.degreeType ?? 'Degree type open'}</span>
                    <span>{thesis.status}</span>
                  </div>
                  <h4 className={styles.resultTitle}>{thesis.title}</h4>
                  <p className={styles.resultChair}>{thesis.chairName ?? 'Chair not specified'}</p>
                  <p className={styles.resultSummary}>{thesis.aiOverview ?? thesis.originalDescription ?? 'No summary available.'}</p>
                </Link>
              ))}
            </div>
            {displayedTheses.length > THESES_PER_PAGE && (
              <nav className={styles.paginationControls} aria-label="Thesis result pages">
                <button
                  className={`${styles.paginationButton} ${styles.clickableButton}`}
                  type="button"
                  aria-label="Previous page"
                  disabled={visiblePage === 1}
                  onClick={() => changePage(Math.max(1, visiblePage - 1))}
                >
                  ←
                </button>
                <label className={styles.paginationMeta}>
                  <span>Page</span>
                  <select
                    className={styles.paginationSelect}
                    aria-label="Select thesis result page"
                    value={visiblePage}
                    onChange={(event) => changePage(Number(event.target.value))}
                  >
                    {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
                      <option key={page} value={page}>
                        {page}
                      </option>
                    ))}
                  </select>
                  <span>out of {totalPages}</span>
                </label>
                <button
                  className={`${styles.paginationButton} ${styles.clickableButton}`}
                  type="button"
                  aria-label="Next page"
                  disabled={visiblePage === totalPages}
                  onClick={() => changePage(Math.min(totalPages, visiblePage + 1))}
                >
                  →
                </button>
              </nav>
            )}
            {isLoadingTheses && <div className={styles.loadingState} role="status"><span className={styles.loadingSpinner} />Refreshing thesis proposals…</div>}
          </>
        ) : isLoadingTheses && !serverSearchResults ? (
          <div className={styles.loadingState} role="status"><span className={styles.loadingSpinner} />Loading thesis proposals…</div>
        ) : (
          <div className={styles.emptyState}>
            <h3 className={styles.emptyTitle}>{serverSearchResults ? 'No matching theses' : 'No thesis proposals loaded'}</h3>
            <p className={styles.emptyText}>
              {serverSearchResults
                ? 'Try changing the natural language query or relaxing the filters.'
                : 'The thesis list is empty or the thesis service could not be reached.'}
            </p>
          </div>
        )}
      </section>
    </main>
  );
}
