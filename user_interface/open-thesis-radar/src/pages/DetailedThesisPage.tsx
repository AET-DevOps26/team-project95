import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styles from '../style/DetailedThesisPage.module.css';
import arrowLeftIcon from '/assets/icons/arrow-left.svg';
import graduationCapIcon from '/assets/icons/graduation-cap.svg';
import notebookIcon from '/assets/icons/notebook.svg';
import circleQuestionMarkIcon from '/assets/icons/circle-question-mark.svg';
import sparklesIcon from '/assets/icons/sparkles.svg';
import fileIcon from '/assets/icons/file.svg';
import infoIcon from '/assets/icons/info.svg';
import externalLinkIcon from '/assets/icons/external-link.svg';
import TopBar from '../components/ui/TopBar';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';
import { getThesisById } from '../api/theses';
import type { components } from '../api';

type ThesisProposal = components['schemas']['ThesisProposal'];

const DEFAULT_THESIS_ID = 1001;
const EMPTY_FIELD = 'Not specified';

function displayValue(value: string | number | null | undefined) {
  if (value === null || value === undefined || value === '') {
    return EMPTY_FIELD;
  }

  return String(value);
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return EMPTY_FIELD;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function formatAdvisors(advisors: ThesisProposal['advisors']) {
  if (!advisors?.length) {
    return EMPTY_FIELD;
  }

  return advisors.map((advisor) => advisor.name || advisor.email || EMPTY_FIELD).join(', ');
}

export default function DetailedThesisPage() {
  const navigate = useNavigate();
  const { thesisId } = useParams();
  const parsedThesisId = Number(thesisId ?? DEFAULT_THESIS_ID);
  const resolvedThesisId = Number.isFinite(parsedThesisId) ? parsedThesisId : DEFAULT_THESIS_ID;
  const [thesis, setThesis] = useState<ThesisProposal | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isCurrent = true;

    async function loadThesis() {
      setIsLoading(true);
      setErrorMessage(null);

      try {
        const response = await getThesisById(resolvedThesisId);

        if (isCurrent) {
          setThesis(response);
        }
      } catch (error) {
        if (isCurrent) {
          setThesis(null);
          setErrorMessage(error instanceof Error ? error.message : 'Failed to load thesis details.');
        }
      } finally {
        if (isCurrent) {
          setIsLoading(false);
        }
      }
    }

    void loadThesis();

    return () => {
      isCurrent = false;
    };
  }, [resolvedThesisId]);

  const sourceUrl = thesis?.sourceUrl;

  return (
    <main className={styles.page}>
      <TopBar brand="Thesis Radar" />

      <section className={styles.content}>
        <Button variant="ghost" onClick={() => navigate('/')} className={styles.backButton}>
          <img src={arrowLeftIcon} alt="" className={styles.backIcon} />
          <span>Back to Results</span>
        </Button>

        {isLoading && (
          <Card className={`${styles.stateCard} ${styles.loadingCard}`} role="status" aria-live="polite">
            <div className={styles.loadingSpinner} aria-hidden="true"></div>
            <div>
              <h1 className={styles.stateTitle}>Loading thesis details</h1>
              <p className={styles.stateText}>Fetching thesis #{resolvedThesisId}.</p>
              <div className={styles.loadingBars} aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </Card>
        )}

        {!isLoading && errorMessage && (
          <Card className={styles.stateCard}>
            <h1 className={styles.stateTitle}>Could not load thesis</h1>
            <p className={styles.stateText}>{errorMessage}</p>
          </Card>
        )}

        {!isLoading && thesis && (
          <>
            <Card className={styles.heroCard}>
              <div className={styles.badge}>{displayValue(thesis.status)}</div>
              <h1 className={styles.title}>{thesis.title}</h1>
              <p className={styles.offeredBy}>
                Offered by <span>{displayValue(thesis.chairName)}</span>
              </p>

              <div className={styles.metaGrid}>
                <div className={styles.metaItem}>
                  <h2 className={styles.labelWithIcon}>
                    <img src={graduationCapIcon} alt="" className={styles.inlineIcon} />
                    <span>Degree Type</span>
                  </h2>
                  <p className={styles.metaValue}>{displayValue(thesis.degreeType)}</p>
                </div>
                <div className={styles.metaItem}>
                  <h2 className={styles.labelWithIcon}>
                    <img src={notebookIcon} alt="" className={styles.inlineIcon} />
                    <span>Research Area</span>
                  </h2>
                  <p className={styles.metaValue}>{displayValue(thesis.researchArea)}</p>
                </div>
                <div className={styles.metaItem}>
                  <h2 className={styles.labelWithIcon}>
                    <img src={infoIcon} alt="" className={styles.inlineIcon} />
                    <span>Last Seen</span>
                  </h2>
                  <p className={styles.metaValue}>{formatDateTime(thesis.lastSeenAt)}</p>
                </div>
                <div className={styles.metaItem}>
                  <h2 className={styles.labelWithIcon}>
                    <img src={circleQuestionMarkIcon} alt="" className={styles.inlineIcon} />
                    <span>Status</span>
                  </h2>
                  <p className={`${styles.metaValue} ${styles.statusAvailable}`}>{displayValue(thesis.status)}</p>
                </div>
              </div>
            </Card>

            <div className={styles.mainGrid}>
              <div className={styles.column}>
                <Card className={styles.sectionCard}>
                  <h2 className={styles.sectionHeading}>
                    <img src={sparklesIcon} alt="" className={styles.inlineIcon} />
                    <span>AI Overview</span>
                  </h2>
                  <p className={styles.sectionText}>{displayValue(thesis.aiOverview)}</p>
                </Card>

                <Card className={styles.sectionCard}>
                  <h2 className={styles.sectionHeading}>
                    <img src={fileIcon} alt="" className={styles.inlineIcon} />
                    <span>Original Description</span>
                  </h2>
                  <p className={styles.sectionText}>{displayValue(thesis.originalDescription)}</p>
                </Card>
              </div>

              <div className={styles.column}>
                <Card className={styles.sectionCard}>
                  <h2 className={styles.sectionHeading}>
                    <img src={infoIcon} alt="" className={styles.inlineIcon} />
                    <span>Additional Information</span>
                  </h2>

                  <div className={styles.additionalGrid}>
                    <div>
                      <h3 className={styles.detailLabel}>Chair</h3>
                      <p className={styles.detailValue}>{displayValue(thesis.chairName)}</p>
                      <h3 className={styles.detailLabel}>Research Area</h3>
                      <p className={styles.detailValue}>{displayValue(thesis.researchArea)}</p>
                      <h3 className={styles.detailLabel}>Degree Type</h3>
                      <p className={styles.detailValue}>{displayValue(thesis.degreeType)}</p>
                    </div>
                    <div>
                      <h3 className={styles.detailLabel}>Advisors</h3>
                      <p className={styles.detailValue}>{formatAdvisors(thesis.advisors)}</p>
                      <h3 className={styles.detailLabel}>Source</h3>
                      <a href={sourceUrl} target="_blank" rel="noreferrer" className={styles.detailLink}>
                        View source
                      </a>
                      <h3 className={styles.detailLabel}>Source URL</h3>
                      <a href={sourceUrl} target="_blank" rel="noreferrer" className={styles.detailLink}>
                        {sourceUrl}
                      </a>
                    </div>
                  </div>
                </Card>
              </div>
            </div>

            <Card className={styles.ctaCard}>
              <div>
                <h2 className={styles.ctaTitle}>Interested in this thesis?</h2>
                <p className={styles.ctaText}>Visit the official source to review details and apply.</p>
              </div>
              <Button href={sourceUrl} target="_blank" rel="noreferrer" className={styles.ctaButton}>
                <span>Visit Source</span>
                <img src={externalLinkIcon} alt="" className={styles.buttonIcon} />
              </Button>
            </Card>
          </>
        )}
      </section>
    </main>
  );
}
