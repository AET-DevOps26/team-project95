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
//     status: 'OPEN',
//     lastSeenAt: '2026-05-22T09:30:00.000Z',
//     advisors: [
//       { id: 101, name: 'Dr. Lena Hoffmann', email: 'lena.hoffmann@example.tum.de' },
//       { id: 102, name: 'Maximilian Weber', email: 'maximilian.weber@example.tum.de' },
//     ],
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
// };

