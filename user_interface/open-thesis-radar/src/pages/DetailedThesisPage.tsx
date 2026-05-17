import styles from '../style/DetailedThesisPage.module.css'
import arrowLeftIcon from "/assets/icons/arrow-left.svg";
import graduationCapIcon from "/assets/icons/graduation-cap.svg";
import locationIcon from "/assets/icons/location.svg";
import notebookIcon from "/assets/icons/notebook.svg";
import circleQuestionMarkIcon from "/assets/icons/circle-question-mark.svg";
import sparklesIcon from "/assets/icons/sparkles.svg";
import fileIcon from "/assets/icons/file.svg";
import infoIcon from "/assets/icons/info.svg";
import externalLinkIcon from "/assets/icons/external-link.svg";
import { useNavigate } from 'react-router-dom'


export default function DetailedThesisPage() {
    const navigate = useNavigate()

    return (
        <main className={styles.page}>
            <header className={styles.topBar}>
                <div className={styles.topBarInner}>
                    <div className={styles.logo}>ThesisHub</div>
                </div>
            </header>

            <section className={styles.content}>
                <button onClick={() => navigate("/") } className={`${styles.backButton} ${styles.clickableButton}`} type="button">
                    <img src={arrowLeftIcon} alt="Back" className={styles.backIcon} />
                    <span>Back to results</span>
                </button>
                <br/>
                <div className={styles.badge}>Open Thesis</div>
                <h1 className={styles.title}>Post-Quantum Crypotography in Intra-Vehicle Networks</h1>
                <p className={styles.offeredBy}>Offered by <span>Chair of Software Engineering</span></p>

                <div className={styles.metaGrid}>
                    <div className={styles.metaItem}>
                        <h3 className={styles.labelWithIcon}>
                            <img src={graduationCapIcon} alt="" className={styles.inlineIcon} />
                            <span>Degree Type</span>
                        </h3>
                        <p>BACHELOR</p>
                    </div>
                    <div className={styles.metaItem}>
                        <h3 className={styles.labelWithIcon}>
                            <img src={notebookIcon} alt="" className={styles.inlineIcon} />
                            <span>Research Area</span>
                        </h3>
                        <p>Networking</p>
                    </div>
                    <div className={styles.metaItem}>
                        <h3 className={styles.labelWithIcon}>
                            <img src={locationIcon} alt="" className={styles.inlineIcon} />
                            <span>Location</span>
                        </h3>
                        <p>Garching bei München</p>
                    </div>
                    <div className={styles.metaItem}>
                        <h3 className={styles.labelWithIcon}>
                            <img src={circleQuestionMarkIcon} alt="" className={styles.inlineIcon} />
                            <span>Status</span>
                        </h3>
                        <p className={styles.statusAvailable}>Available</p>
                    </div>
                </div>

                <div className={styles.sectionCard}>
                    <h2 className={styles.sectionHeading}>
                        <img src={sparklesIcon} alt="" className={styles.inlineIcon} />
                        <span>AI Overview</span>
                    </h2>
                    <p>
                        This thesis focuses on developing and evaluating semantic search methods to improve
                        the discovery of open thesis opportunities. The goal is to go beyond keyword matching
                        by understanding the meaning and context of thesis descriptions.
                    </p>
                    <p>
                        The work includes exploring semantic embedding models, building a prototype search
                        system, and evaluating results based on relevance and user feedback.
                    </p>
                </div>

                <div className={styles.sectionCard}>
                    <h2 className={styles.sectionHeading}>
                        <img src={fileIcon} alt="" className={styles.inlineIcon} />
                        <span>Original Description</span>
                    </h2>
                    <p>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer porta, nunc vitae
                        aliquet accumsan, nisl nisi ultricies justo, nec sollicitudin lacus nisl eget justo.
                        Sed commodo, arcu eget bibendum tincidunt, nisl nisl sollicitudin nisl, eget aliquam
                        nisl nisl eget nisl.
                    </p>
                </div>

                <div className={styles.sectionCard}>
                    <h2 className={styles.sectionHeading}>
                        <img src={infoIcon} alt="" className={styles.inlineIcon} />
                        <span>Additional Information</span>
                    </h2>
                    <div className={styles.additionalGrid}>
                        <div>
                            <h3>Chair</h3>
                            <p>Chair of Software Engineering</p>
                            <h3>Research Area (Detailed)</h3>
                            <p>Artificial Intelligence</p>
                            <h3>Degree Type</h3>
                            <p>MASTER</p>
                            <h3>Location</h3>
                            <p>Garching bei München</p>
                        </div>
                        <div>
                            <h3>Language</h3>
                            <p>English</p>
                            <h3>Application Deadline</h3>
                            <p>Open</p>
                            <h3>Source</h3>
                            <a href="#">View on TUM Website</a>
                            <h3>Source URL</h3>
                            <a href="#">https://www.in.tum.de/en/open-theses/1234</a>
                        </div>
                    </div>
                </div>

                <div className={styles.bottomCta}>
                    <div>
                        <h2>Interested in this thesis?</h2>
                        <p>To apply or learn more about this thesis, please visit the official source.</p>
                    </div>
                    <button className={`${styles.visitSourceButton} ${styles.clickableButton}`} type="button">
                        <span>Visit Source</span>
                        <img src={externalLinkIcon} alt="" className={styles.buttonIcon} />
                    </button>
                </div>
            </section>
        </main>
    )
}
