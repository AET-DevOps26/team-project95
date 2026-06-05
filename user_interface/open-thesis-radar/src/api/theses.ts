import type { components, operations } from '../api';
// import { USE_MOCK_THESIS } from '../config';
// import { MOCK_THESES } from '../mocks/theses';
import { defaultApiClient, type ApiClient } from './client';

type SearchThesesRequest = operations['searchTheses']['requestBody']['content']['application/json'];
type SearchThesesResponse = operations['searchTheses']['responses'][200]['content']['application/json'];
type ThesisProposal = components['schemas']['ThesisProposal'];

// function thesisMatchesFilters(thesis: ThesisProposal, filters?: SearchThesesRequest['filters']) {
//   if (!filters) {
//     return true;
//   }
//
//   const matchesChair = !filters.chairIds?.length || filters.chairIds.includes(thesis.chairId);
//   const matchesDegree = !filters.degreeTypes?.length || Boolean(thesis.degreeType && filters.degreeTypes.includes(thesis.degreeType));
//   const matchesResearchArea =
//     !filters.researchAreas?.length || Boolean(thesis.researchArea && filters.researchAreas.includes(thesis.researchArea));
//   const matchesStatus = !filters.status || thesis.status === filters.status;
//   const matchesTags = !filters.tags?.length || filters.tags.every((tag) => thesis.tags?.includes(tag));
//
//   return matchesChair && matchesDegree && matchesResearchArea && matchesStatus && matchesTags;
// }
//
// function thesisMatchesNaturalLanguage(thesis: ThesisProposal, query?: string | null) {
//   const normalizedQuery = query?.trim().toLowerCase();
//
//   if (!normalizedQuery) {
//     return true;
//   }
//
//   const searchableText = [
//     thesis.title,
//     thesis.chairName,
//     thesis.degreeType,
//     thesis.researchArea,
//     thesis.aiOverview,
//     thesis.originalDescription,
//     ...(thesis.tags ?? []),
//     ...(thesis.advisors?.map((advisor) => `${advisor.name ?? ''} ${advisor.email ?? ''}`) ?? []),
//   ]
//     .filter(Boolean)
//     .join(' ')
//     .toLowerCase();
//
//   return normalizedQuery
//     .split(/\s+/)
//     .filter(Boolean)
//     .every((token) => searchableText.includes(token));
// }

export async function listTheses(client: ApiClient = defaultApiClient): Promise<ThesisProposal[]> {
  // if (USE_MOCK_THESIS) {
  //   return MOCK_THESES;
  // }

  return client.request<ThesisProposal[]>('/api/v1/theses', {
    method: 'GET',
  });
}

export async function searchTheses(
  request: SearchThesesRequest,
  client: ApiClient = defaultApiClient,
): Promise<SearchThesesResponse> {
  // if (USE_MOCK_THESIS) {
  //   const filteredTheses = MOCK_THESES.filter(
  //     (thesis) => thesisMatchesFilters(thesis, request.filters) && thesisMatchesNaturalLanguage(thesis, request.naturalLanguageQuery),
  //   );
  //
  //   return {
  //     items: filteredTheses.map((thesis) => ({ ...thesis, semanticScore: request.naturalLanguageQuery ? 0.9 : null })),
  //     page: request.page,
  //     size: request.size,
  //     totalElements: filteredTheses.length,
  //   };
  // }

  return client.request<SearchThesesResponse>('/api/v1/theses/search', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function getThesisById(
  thesisId: number,
  client: ApiClient = defaultApiClient,
): Promise<ThesisProposal> {
  // if (USE_MOCK_THESIS) {
  //   const thesis = MOCK_THESES.find((candidate) => candidate.id === thesisId) ?? MOCK_THESES[0];
  //
  //   return thesis;
  // }

  return client.request<ThesisProposal>(`/api/v1/theses/${thesisId}`, {
    method: 'GET',
  });
}
