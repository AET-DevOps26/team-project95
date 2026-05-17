import { useState } from 'react'
import styles from '../style/HomePage.module.css'
import scrollIcon from "/assets/icons/chevrons-down.svg";

type QueryMode = 'Natural Language' | 'Filters' | 'Both'

export default function HomePage() {
    const [queryMode, setQueryMode] = useState<QueryMode>('Both')
    const [naturalLanguageQuery, setNaturalLanguageQuery] = useState('')
    const showSearchBar = queryMode === 'Natural Language' || queryMode === 'Both'
    const showFilters = queryMode === 'Filters' || queryMode === 'Both'

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
                    Discover open thesis topics from TUM's chairs. Use natural-language
                    search or powerful filters to find opportunities that match your interests and goals.
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
                    Describe what you're looking for or use filters to find the right opportunities.
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
                        <button className={`${styles.resetLink} ${styles.clickableButton}`} type="button">
                            Reset all
                        </button>
                    </div>
                    {/*Placeholders for now: */}
                    <div className={styles.filtersGrid}>
                        <div className={styles.filterBox}>Degree</div>
                        <div className={styles.filterBox}>Research Area</div>
                        <div className={styles.filterBox}>Chair</div>
                        <div className={styles.filterBox}>Topic</div>
                        <div className={styles.filterBox}>Language</div>
                        <div className={styles.filterBox}>Semester</div>
                        <div className={styles.filterBox}>Location</div>
                        <div className={styles.filterBox}>Thesis Type</div>
                    </div>
                </div>

                <div className={styles.emptyState}>
                {/*<div className={styles.emptyIcon}>◯</div>*/}
                    <h3 className={styles.emptyTitle}>No results yet</h3>
                    <p className={styles.emptyText}>
                        Use the search box above or apply filters to find open thesis opportunities.
                    </p>
                </div>
            </section>
        </main>
    )
}
