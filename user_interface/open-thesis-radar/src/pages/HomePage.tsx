import { useEffect, useMemo, useRef, useState } from 'react';
import styles from '../style/HomePage.module.css';
import scrollIcon from '/assets/icons/chevrons-down.svg';
import FilterDropdown from '../components/FilterDropdown';
import { getAvailableFilters } from '../api/filters';
import { listTheses, searchTheses } from '../api/theses';
import type { components } from '../api';
import { MOCK_FILTERS } from '../mocks/theses';
import { useSearchState } from '../state/searchState';
import { Link } from 'react-router-dom';

type ThesisProposal = components['schemas']['ThesisProposal'];

const INITIAL_FILTERS: components['schemas']['AvailableFiltersResponse'] = MOCK_FILTERS;

function thesisMatchesSelectedFilters(thesis: ThesisProposal, selectedFilters: components['schemas']['ThesisSearchFilters']) {
  const matchesChair = !selectedFilters.chairIds?.length || selectedFilters.chairIds.includes(thesis.chairId);
  const matchesDegree =
    !selectedFilters.degreeTypes?.length || Boolean(thesis.degreeType && selectedFilters.degreeTypes.includes(thesis.degreeType));
  const matchesResearchArea =
    !selectedFilters.researchAreas?.length ||
    Boolean(thesis.researchArea && selectedFilters.researchAreas.includes(thesis.researchArea));
  const matchesTags = !selectedFilters.tags?.length || selectedFilters.tags.every((tag) => thesis.tags?.includes(tag));

  return matchesChair && matchesDegree && matchesResearchArea && matchesTags;
}

export default function HomePage() {
  const [filters, setFilters] = useState<components['schemas']['AvailableFiltersResponse']>(INITIAL_FILTERS);
  const [allTheses, setAllTheses] = useState<ThesisProposal[]>([]);
  const [serverSearchResults, setServerSearchResults] = useState<ThesisProposal[] | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [resultsError, setResultsError] = useState<string | null>(null);
  const searchSectionRef = useRef<HTMLElement | null>(null);
  const {
    queryMode,
    setQueryMode,
    naturalLanguageQuery,
    setNaturalLanguageQuery,
    selectedFilters,
    setSelectedFilters,
    resetSelectedFilters,
  } = useSearchState();
  const showSearchBar = queryMode === 'Natural Language' || queryMode === 'Both';
  const showFilters = queryMode === 'Filters' || queryMode === 'Both';
  const normalizedNaturalLanguageQuery = naturalLanguageQuery.trim();
  const selectedApiFilters = useMemo(
    () => ({
      chairIds: selectedFilters.chairIds,
      degreeTypes: selectedFilters.degreeTypes,
      researchAreas: selectedFilters.researchAreas,
      tags: selectedFilters.tags,
      status: 'OPEN',
    }),
    [selectedFilters],
  );
  const shouldUseServerSearch = showSearchBar && normalizedNaturalLanguageQuery.length > 0;
  const clientFilteredTheses = useMemo(
    () => allTheses.filter((thesis) => thesisMatchesSelectedFilters(thesis, selectedApiFilters)),
    [allTheses, selectedApiFilters],
  );
  const displayedTheses = serverSearchResults ?? clientFilteredTheses;

  const scrollToSearch = () => {
    searchSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

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
      setResultsError(null);

      try {
        const response = await listTheses();
        setAllTheses(response);
      } catch (error) {
        console.error('Failed to load theses', error);
        setResultsError(error instanceof Error ? error.message : 'Failed to load thesis proposals.');
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
      (filters.chairs ?? []).map((chair) => ({
        value: String(chair.id),
        label: chair.name,
      })),
    [filters.chairs],
  );

  const degreeTypeOptions = useMemo(
    () => (filters.degreeTypes ?? []).map((degreeType) => ({ value: degreeType, label: degreeType })),
    [filters.degreeTypes],
  );

  const researchAreaOptions = useMemo(
    () =>
      (filters.researchAreas ?? []).map((researchArea) => ({
        value: researchArea,
        label: researchArea,
      })),
    [filters.researchAreas],
  );

  const tagOptions = useMemo(
    () => (filters.tags ?? []).map((tag) => ({ value: tag, label: tag })),
    [filters.tags],
  );

  return (
    <main className={styles.page}>
      <header className={styles.topBar}>
        <div className={styles.topBarInner}>
          <div className={styles.logo}>Thesis Radar</div>
          <nav className={styles.topNav} aria-label="Home page navigation">
            <button className={styles.navButton} type="button" onClick={scrollToSearch}>
              Search
            </button>
            <Link className={styles.navButton} to="/thesis">
              Mock thesis
            </Link>
          </nav>
        </div>
      </header>

      <section className={styles.hero}>
        <div className={styles.heroGrid}>
          <div className={styles.heroCopy}>
            <p className={styles.heroEyebrow}>TUM thesis discovery</p>
            <h1 className={styles.heroTitle}>Search open theses by fit.</h1>
            <p className={styles.heroSubtitle}>
              Compare thesis offers across chairs, degree types, research areas, and tags without losing your filter
              context while you inspect a topic.
            </p>
            <div className={styles.heroActions}>
              <button className={`${styles.primaryAction} ${styles.clickableButton}`} type="button" onClick={scrollToSearch}>
                Search theses
              </button>
              <Link className={`${styles.secondaryAction} ${styles.clickableButton}`} to="/thesis">
                Open mock thesis
              </Link>
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

        <div className={styles.modeTabs}>
          <button
            className={`${styles.clickableButton} ${queryMode === 'Natural Language' ? styles.activeTab : styles.tab}`}
            onClick={() => setQueryMode('Natural Language')}
            type="button"
          >
            Natural Language
          </button>
          <button
            className={`${styles.clickableButton} ${queryMode === 'Both' ? styles.activeTab : styles.tab}`}
            onClick={() => setQueryMode('Both')}
            type="button"
          >
            Both
          </button>
          <button
            className={`${styles.clickableButton} ${queryMode === 'Filters' ? styles.activeTab : styles.tab}`}
            onClick={() => setQueryMode('Filters')}
            type="button"
          >
            Filters
          </button>
        </div>

        <form
          className={`${styles.searchBarBlock} ${styles.animatedSection} ${showSearchBar ? styles.isVisible : styles.isHidden}`}
          aria-hidden={!showSearchBar}
          inert={!showSearchBar}
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

        <div
          className={`${styles.filtersCard} ${styles.animatedSection} ${showFilters ? styles.isVisible : styles.isHidden}`}
          aria-hidden={!showFilters}
          inert={!showFilters}
        >
          <div className={styles.filtersHeader}>
            <h3 className={styles.filtersTitle}>Filters</h3>
            <button
              className={`${styles.resetLink} ${styles.clickableButton}`}
              type="button"
              onClick={resetSelectedFilters}
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
            <FilterDropdown
              label="Tag"
              values={selectedFilters.tags}
              options={tagOptions}
              onChange={(values) => setSelectedFilters((prev) => ({ ...prev, tags: values }))}
            />
          </div>
        </div>

        <div className={styles.resultsHeader}>
          <div>
            <h3 className={styles.resultsTitle}>Thesis proposals</h3>
            <p className={styles.resultsMeta}>
              {isSearching ? 'Searching thesis service…' : `${displayedTheses.length} thesis proposals shown`}
            </p>
          </div>
        </div>

        {resultsError && <p className={styles.resultsError}>{resultsError}</p>}

        {displayedTheses.length > 0 ? (
          <div className={styles.resultsGrid}>
            {displayedTheses.map((thesis) => (
              <Link className={styles.resultCard} to={`/thesis/${thesis.id}`} key={thesis.id}>
                <div className={styles.resultTopline}>
                  <span>{thesis.degreeType ?? 'Degree type open'}</span>
                  <span>{thesis.status}</span>
                </div>
                <h4 className={styles.resultTitle}>{thesis.title}</h4>
                <p className={styles.resultChair}>{thesis.chairName ?? 'Chair not specified'}</p>
                <p className={styles.resultSummary}>{thesis.aiOverview ?? thesis.originalDescription ?? 'No summary available.'}</p>
                <div className={styles.resultTags}>
                  {(thesis.tags ?? []).slice(0, 4).map((tag) => (
                    <span className={styles.resultTag} key={tag}>{tag}</span>
                  ))}
                </div>
              </Link>
            ))}
          </div>
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
