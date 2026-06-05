// import type { components, operations } from '../api';
//
// type ThesisProposal = components['schemas']['ThesisProposal'];
// type SearchThesesResponse = operations['searchTheses']['responses'][200]['content']['application/json'];
// type AvailableFiltersResponse = components['schemas']['AvailableFiltersResponse'];
//
// export const MOCK_THESES: ThesisProposal[] = [
//   {
//     id: 1001,
//     chairId: 1,
//     chairName: 'Chair of Software Engineering',
//     title: 'Post-Quantum Cryptography in Intra-Vehicle Networks',
//     degreeType: 'BACHELOR',
//     originalDescription:
//       'Analyze candidate post-quantum cryptographic schemes, evaluate their integration constraints in automotive buses, and prototype a practical architecture for low-latency embedded environments.',
//     aiOverview:
//       'This thesis investigates secure communication for intra-vehicle networks under post-quantum threat models, with emphasis on practical latency and deployment constraints.',
//     researchArea: 'Security',
//     sourceUrl: 'https://www.in.tum.de/en/open-theses/post-quantum-vehicle-networks',
//     rawHtmlSnapshot:
//       '<article><h1>Post-Quantum Cryptography in Intra-Vehicle Networks</h1><p>Evaluate PQC schemes for automotive communication.</p></article>',
//     extractionConfidence: 0.93,
//     status: 'OPEN',
//     lastSeenAt: '2026-05-22T09:30:00.000Z',
//     advisors: [
//       { id: 101, name: 'Dr. Lena Hoffmann', email: 'lena.hoffmann@example.tum.de' },
//       { id: 102, name: 'Maximilian Weber', email: 'maximilian.weber@example.tum.de' },
//     ],
//     tags: ['Security', 'Embedded Systems', 'Automotive', 'Cryptography'],
//   },
//   {
//     id: 1002,
//     chairId: 2,
//     chairName: 'Chair of Machine Learning',
//     title: 'Semantic Search for Cross-Chair Thesis Discovery',
//     degreeType: 'MASTER',
//     originalDescription:
//       'Design and evaluate a retrieval pipeline that combines structured thesis metadata with vector search over descriptions from multiple university chairs.',
//     aiOverview:
//       'This thesis explores hybrid information retrieval for thesis recommendations, combining filters, embeddings, ranking, and evaluation with realistic student queries.',
//     researchArea: 'Artificial Intelligence',
//     sourceUrl: 'https://www.in.tum.de/en/open-theses/semantic-search-thesis-discovery',
//     rawHtmlSnapshot:
//       '<article><h1>Semantic Search for Cross-Chair Thesis Discovery</h1><p>Build a hybrid retrieval system for thesis proposals.</p></article>',
//     extractionConfidence: 0.91,
//     status: 'OPEN',
//     lastSeenAt: '2026-05-24T14:15:00.000Z',
//     advisors: [
//       { id: 201, name: 'Prof. Anna Keller', email: 'anna.keller@example.tum.de' },
//       { id: 202, name: 'Jonas Richter', email: 'jonas.richter@example.tum.de' },
//     ],
//     tags: ['LLM', 'Semantic Search', 'Information Retrieval', 'MLOps'],
//   },
//   {
//     id: 1003,
//     chairId: 3,
//     chairName: 'Chair of Robotics and AI',
//     title: 'Vision-Language Planning for Assistive Mobile Robots',
//     degreeType: 'MASTER',
//     originalDescription:
//       'Investigate how vision-language models can produce robust task plans for assistive robots operating in cluttered indoor environments.',
//     aiOverview:
//       'The project studies robot task planning from natural language instructions and visual scene context, including failure handling and evaluation in simulation.',
//     researchArea: 'Robotics',
//     sourceUrl: 'https://www.in.tum.de/en/open-theses/vision-language-planning-assistive-robots',
//     rawHtmlSnapshot:
//       '<article><h1>Vision-Language Planning for Assistive Mobile Robots</h1><p>Use VLMs for grounded robot task planning.</p></article>',
//     extractionConfidence: 0.88,
//     status: 'OPEN',
//     lastSeenAt: '2026-05-26T11:45:00.000Z',
//     advisors: [
//       { id: 301, name: 'Dr. Sofia Brandt', email: 'sofia.brandt@example.tum.de' },
//       { id: 302, name: 'Nina Schneider', email: 'nina.schneider@example.tum.de' },
//     ],
//     tags: ['Computer Vision', 'Robotics', 'Natural Language Processing', 'Planning'],
//   },
// ];
//
// export const MOCK_SEARCH_RESPONSE: SearchThesesResponse = {
//   items: MOCK_THESES.map((thesis, index) => ({
//     ...thesis,
//     semanticScore: [0.94, 0.9, 0.86][index] ?? null,
//   })),
//   page: 0,
//   size: MOCK_THESES.length,
//   totalElements: MOCK_THESES.length,
// };
//
// export const MOCK_FILTERS: AvailableFiltersResponse = {
//   chairs: Array.from(new Map(MOCK_THESES.map((thesis) => [thesis.chairId, {
//     id: thesis.chairId,
//     name: thesis.chairName ?? `Chair ${thesis.chairId}`,
//     websiteUrl: `https://example.org/chairs/${thesis.chairId}`,
//   }])).values()),
//   degreeTypes: Array.from(new Set(MOCK_THESES.map((thesis) => thesis.degreeType).filter(Boolean))) as string[],
//   researchAreas: Array.from(new Set(MOCK_THESES.map((thesis) => thesis.researchArea).filter(Boolean))) as string[],
//   tags: Array.from(new Set(MOCK_THESES.flatMap((thesis) => thesis.tags ?? []))),
// };
