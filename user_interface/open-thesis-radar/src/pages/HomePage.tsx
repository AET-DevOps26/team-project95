import { useEffect, useMemo, useState } from 'react';
import styles from '../style/HomePage.module.css';
import scrollIcon from '/assets/icons/chevrons-down.svg';
import FilterDropdown from '../components/FilterDropdown';
import { getAvailableFilters } from '../api/filters';
import type { components } from '../api';
import { useSearchState } from '../state/searchState';
import { Link } from 'react-router-dom';

const INITIAL_FILTERS: components['schemas']['AvailableFiltersResponse'] = {
  chairs: [
    { id: 1, name: 'Chair of Software Engineering', websiteUrl: 'https://example.org/chairs/se' },
    { id: 2, name: 'Chair of Machine Learning', websiteUrl: 'https://example.org/chairs/ml' },
    { id: 3, name: 'Chair of Robotics and AI', websiteUrl: 'https://example.org/chairs/rai' },
    { id: 4, name: 'Chair of Computer Vision', websiteUrl: 'https://example.org/chairs/cv' },
    { id: 5, name: 'Chair of Data Engineering', websiteUrl: 'https://example.org/chairs/de' },
  ],
  degreeTypes: ['BACHELOR', 'MASTER', 'RESEARCH INTERNSHIP', 'SEMINAR', 'PHD'],
  researchAreas: [
    'Artificial Intelligence',
    'Machine Learning',
    'Natural Language Processing',
    'Computer Vision',
    'Distributed Systems',
  ],
  tags: ['LLM', 'Semantic Search', 'Information Retrieval', 'MLOps', 'Data Mining'],
};

export default function HomePage() {
  const [filters, setFilters] = useState<components['schemas']['AvailableFiltersResponse']>(INITIAL_FILTERS);
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

  useEffect(() => {
    async function loadFilters() {
      try {
        const response = await getAvailableFilters();
        setFilters(response);
      } catch (error) {
        console.error('Failed to load available filters', error);
      }
    }

    void loadFilters();
  }, []);

  useEffect(() => {
    console.log('Currently selected filters:', selectedFilters);
  }, [selectedFilters]);

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
          <div className={styles.logo}>ThesisHub</div>
        </div>
      </header>

      <section className={styles.hero}>
        <h1 className={styles.heroTitle}>Find the Right Thesis for You</h1>
        <p className={styles.heroSubtitle}>
          Discover open thesis topics from TUM&apos;s chairs. Use natural-language search or powerful filters to
          find opportunities that match your interests and goals.
        </p>
        <div className={styles.heroActions}>
          <button className={`${styles.primaryAction} ${styles.clickableButton}`} type="button">
            Explore theses
          </button>
          <button className={`${styles.secondaryAction} ${styles.clickableButton}`} type="button">
            How it works
          </button>
        </div>
        <div className={styles.scrollHint}>
          <div className={styles.scrollHintText}>Scroll to search</div>
          <img src={scrollIcon} alt="Search" className={styles.scrollHintIcon} />
        </div>
      </section>

      <section className={styles.searchSection}>
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
          onSubmit={(event) => event.preventDefault()}
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

        <div className={styles.mockNavigationRow}>
          <Link className={`${styles.mockThesisLink} ${styles.clickableButton}`} to="/thesis">
            Open mock thesis page
          </Link>
        </div>

        <div className={styles.emptyState}>
          <h3 className={styles.emptyTitle}>No results yet</h3>
          <p className={styles.emptyText}>
            Use the search box above or apply filters to find open thesis opportunities.
          </p>
        </div>
      </section>
    </main>
  );
}
