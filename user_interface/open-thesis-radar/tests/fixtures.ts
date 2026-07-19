export const mockTheses = [
  {
    id: 1001,
    title: 'Robot Perception for Warehouse Navigation',
    chairId: 1,
    chairName: 'Chair of Robotics',
    degreeType: 'Master',
    researchArea: 'Robotics',
    status: 'OPEN',
    aiOverview: 'Explore perception methods for autonomous warehouse robots.',
    originalDescription: 'This thesis studies robot perception in warehouses.',
    semanticScore: null,
  },
  {
    id: 1002,
    title: 'Energy-Aware Cloud Scheduling',
    chairId: 2,
    chairName: 'Chair of Distributed Systems',
    degreeType: 'Bachelor',
    researchArea: 'Cloud Computing',
    status: 'OPEN',
    aiOverview: 'Investigate scheduling policies for lower cloud energy usage.',
    originalDescription: 'This thesis focuses on cloud resource scheduling.',
    semanticScore: null,
  },
];

export const semanticSearchThesis = {
  id: 1003,
  title: 'Explainable AI for Medical Decision Support',
  chairId: 3,
  chairName: 'Chair of Responsible AI',
  degreeType: 'Master',
  researchArea: 'Artificial Intelligence',
  status: 'OPEN',
  aiOverview: 'Study explanations for clinical AI recommendations.',
  originalDescription: 'This thesis evaluates explainability methods in healthcare.',
  semanticScore: 0.92,
};

export const mockFilters = {
  chairs: [
    { id: 1, name: 'Chair of Robotics' },
    { id: 2, name: 'Chair of Distributed Systems' },
    { id: 3, name: 'Chair of Responsible AI' },
  ],
  degreeTypes: ['Bachelor', 'Master'],
  researchAreas: ['Robotics', 'Cloud Computing', 'Artificial Intelligence'],
};

export const mockThesisDetail = {
  ...mockTheses[0],
  advisors: [
    { name: 'Dr. Ada Lovelace', email: 'ada.lovelace@example.edu' },
  ],
  sourceUrl: 'https://example.edu/theses/robot-perception',
  lastSeenAt: '2026-06-15T10:00:00Z',
};
