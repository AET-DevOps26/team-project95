WITH seeded_chairs(name, website_url) AS (
    VALUES
        ('Chair of Computer Architecture and Parallel Systems', 'https://www.ce.cit.tum.de/en/caps/theses/open/'),
        ('Chair of Communications Engineering', 'https://www.ce.cit.tum.de/en/lnt/student-thesesseminars/offered-theses/'),
        ('Chair of Communication Networks', 'https://www.ce.cit.tum.de/en/lkn/student-thesis/')
        -- ('Chair of 3D Artificial Intelligence', 'https://www.3dunderstanding.org/masters-topics/'),
        -- ('Chair of AI Processor Design', 'https://www.ce.cit.tum.de/en/aipro/thesis/'),
        -- ('Chair of Decision Sciences & Systems', 'https://www.cs.cit.tum.de/en/dss/teaching/theses-topics/'),
        -- ('Chair of Circuit Design', 'https://www.ee.cit.tum.de/en/lse/theses-and-internships/master-theses/'),
        -- ('Chair of Computational Molecular Medicine', 'https://www.cs.cit.tum.de/cmm/jobs-theses/'),
        -- ('Chair of Computational Photonics', 'https://www.ee.cit.tum.de/cph/teaching/theses/'),
        -- ('Chair of Computer Architecture and Parallel Systems', 'https://www.ce.cit.tum.de/en/caps/theses/open/'),
        -- ('Chair of Connected Mobility', 'https://www.ce.cit.tum.de/cm/thesis-guided-research/open-thesis-topics-guided-research/'),
        -- ('Chair of Control and Manipulation of Microscale Living Objects', 'https://www.ee.cit.tum.de/mml/open-positions/'),
        -- ('Chair of Cyber Physical Systems', 'https://www.ce.cit.tum.de/en/cps/open-student-theses/'),
        -- ('Chair of Data Analytics and Statistics', 'https://www.cs.cit.tum.de/dast/lehre/projects/'),
        -- ('Chair of Data Analytics and Machine Learning', 'https://www.cs.cit.tum.de/en/daml/open-theses/'),
        -- ('Chair of Database Systems', 'https://db.in.tum.de/teaching/theses/?lang=en'),
        -- ('Chair of Efficient Algorithms', 'https://www.cs.cit.tum.de/algo/theses-and-projects/'),
        -- ('Chair of Electronic Design Automation', 'https://www.ce.cit.tum.de/en/eda/theses-jobs/open-positions/'),
        -- ('Chair of Environmental Sensing and Modeling', 'https://www.ee.cit.tum.de/en/esm/studentische-arbeiten/bachelors-masters-thesis/'),
        -- ('Chair of Formal Languages, Compiler Construction, Software Construction', 'https://www.cs.cit.tum.de/pl/lehre/studienarbeiten/'),
        -- ('Chair of High-Frequency Engineering', 'https://www.ee.cit.tum.de/en/hft/education/theses/'),
        -- ('Chair of IT Security', 'https://www.sec.in.tum.de/i20/student-work'),
        -- ('Chair of Information Infrastructures', 'https://www.cs.cit.tum.de/en/ii/teaching/final-thesis/available-thesis-topics/'),
        -- ('Chair of Information Systems and Business Process Management', 'https://www.cs.cit.tum.de/en/bpm/theses/'),
        -- ('Chair of Information-oriented Control', 'https://www.ce.cit.tum.de/en/itr/student-projects-and-theses/theses/'),
        -- ('Chair of Explainable Machine Learning', 'https://www.eml-munich.de/teaching/thesis'),
        -- ('Chair of Legal Tech', 'https://tumlegaltech.github.io/thesis_mentoring/'),
        -- ('Chair of Machine Learning', 'https://www.ce.cit.tum.de/mli/theses/'),
        -- ('Chair of Media Technology', 'https://www.ce.cit.tum.de/en/lmt/student-thesis/'),
        -- ('Chair of Microsensors and Actuators', 'https://www.ee.cit.tum.de/en/msa/teaching/theses-and-internships/'),
        -- ('Chair of Network Architectures and Services', 'https://www.net.in.tum.de/theses/'),
        -- ('Chair of Perception for Intelligent Systems', 'https://www.ce.cit.tum.de/pins/open-positions/student-positions/'),
        -- ('Chair of Quantum Communication Systems Engineering', 'https://www.ce.cit.tum.de/qcs/open-positions/'),
        -- ('Chair of Robotic, Artificial Intelligence and Real-Time Systems', 'https://www.ce.cit.tum.de/air/thesis-proposals/'),
        -- ('Chair of Security in Information Technology', 'https://www.ce.cit.tum.de/eisec/studentische-arbeiten/'),
        -- ('Chair of Software Engineering and AI', 'https://www.cs.cit.tum.de/seai/abschlussarbeiten/'),
        -- ('Chair of Software and Systems Engineering', 'https://www.cs.cit.tum.de/en/sse/teaching/theses-and-projects/'),
        -- ('Chair of Theoretical Information Technology', 'https://www.ce.cit.tum.de/en/lti/studentische-arbeiten/#c55667'),
        -- ('Chair of Visual Computing and Artificial Intelligence', 'https://niessner.github.io/student-topics/')
),
inserted_chairs AS (
    INSERT INTO chairs (name, website_url)
    SELECT name, website_url
    FROM seeded_chairs
    RETURNING id, website_url
)
INSERT INTO source_endpoints (url, status, chair_id)
SELECT website_url, 'ACTIVE', id
FROM inserted_chairs;
