import styles from '../style/DetailedThesisPage.module.css';
import arrowLeftIcon from '/assets/icons/arrow-left.svg';
import graduationCapIcon from '/assets/icons/graduation-cap.svg';
import locationIcon from '/assets/icons/location.svg';
import notebookIcon from '/assets/icons/notebook.svg';
import circleQuestionMarkIcon from '/assets/icons/circle-question-mark.svg';
import sparklesIcon from '/assets/icons/sparkles.svg';
import fileIcon from '/assets/icons/file.svg';
import infoIcon from '/assets/icons/info.svg';
import externalLinkIcon from '/assets/icons/external-link.svg';
import { useNavigate } from 'react-router-dom';
import TopBar from '../components/ui/TopBar';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';

export default function DetailedThesisPage() {
  const navigate = useNavigate();
  const sourceUrl = 'https://www.in.tum.de/en/open-theses/1234';

  return (
    <main className={styles.page}>
      <TopBar brand="Thesis Radar" />

      <section className={styles.content}>
        <Button variant="ghost" onClick={() => navigate('/')} className={styles.backButton}>
          <img src={arrowLeftIcon} alt="" className={styles.backIcon} />
          <span>Back to Results</span>
        </Button>

        <Card className={styles.heroCard}>
          <div className={styles.badge}>Open Thesis</div>
          <h1 className={styles.title}>Post-Quantum Cryptography in Intra-Vehicle Networks</h1>
          <p className={styles.offeredBy}>
            Offered by <span>Chair of Software Engineering</span>
          </p>

          <div className={styles.metaGrid}>
            <div className={styles.metaItem}>
              <h2 className={styles.labelWithIcon}>
                <img src={graduationCapIcon} alt="" className={styles.inlineIcon} />
                <span>Degree Type</span>
              </h2>
              <p className={styles.metaValue}>BACHELOR</p>
            </div>
            <div className={styles.metaItem}>
              <h2 className={styles.labelWithIcon}>
                <img src={notebookIcon} alt="" className={styles.inlineIcon} />
                <span>Research Area</span>
              </h2>
              <p className={styles.metaValue}>Networking</p>
            </div>
            <div className={styles.metaItem}>
              <h2 className={styles.labelWithIcon}>
                <img src={locationIcon} alt="" className={styles.inlineIcon} />
                <span>Location</span>
              </h2>
              <p className={styles.metaValue}>Garching bei Muenchen</p>
            </div>
            <div className={styles.metaItem}>
              <h2 className={styles.labelWithIcon}>
                <img src={circleQuestionMarkIcon} alt="" className={styles.inlineIcon} />
                <span>Status</span>
              </h2>
              <p className={`${styles.metaValue} ${styles.statusAvailable}`}>Available</p>
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
              <p className={styles.sectionText}>
                This thesis focuses on developing and evaluating semantic search methods to improve discovery of open
                thesis opportunities. The goal is to go beyond keyword matching by modeling meaning and context.
              </p>
              <p className={styles.sectionText}>
                The work includes exploring semantic embedding models, building a prototype search workflow, and
                evaluating relevance with student-facing retrieval benchmarks.
              </p>
            </Card>

            <Card className={styles.sectionCard}>
              <h2 className={styles.sectionHeading}>
                <img src={fileIcon} alt="" className={styles.inlineIcon} />
                <span>Original Description</span>
              </h2>
              <p className={styles.sectionText}>
                This thesis investigates secure communication for intra-vehicle networks under post-quantum threat
                models. The objective is to analyze candidate cryptographic schemes, evaluate integration constraints,
                and prototype a practical architecture for low-latency embedded environments.
              </p>
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
                  <p className={styles.detailValue}>Chair of Software Engineering</p>
                  <h3 className={styles.detailLabel}>Research Area (Detailed)</h3>
                  <p className={styles.detailValue}>Artificial Intelligence</p>
                  <h3 className={styles.detailLabel}>Degree Type</h3>
                  <p className={styles.detailValue}>MASTER</p>
                  <h3 className={styles.detailLabel}>Location</h3>
                  <p className={styles.detailValue}>Garching bei Muenchen</p>
                </div>
                <div>
                  <h3 className={styles.detailLabel}>Language</h3>
                  <p className={styles.detailValue}>English</p>
                  <h3 className={styles.detailLabel}>Application Deadline</h3>
                  <p className={styles.detailValue}>Open</p>
                  <h3 className={styles.detailLabel}>Source</h3>
                  <a href={sourceUrl} target="_blank" rel="noreferrer" className={styles.detailLink}>
                    View on TUM Website
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
      </section>
    </main>
  );
}
