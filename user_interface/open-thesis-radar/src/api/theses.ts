import type { components, operations } from '../api';
// import { USE_MOCK_THESIS } from '../config';
// import { MOCK_THESES } from '../mocks/theses';
import { defaultApiClient, type ApiClient } from './client';

type SearchThesesRequest = operations['searchTheses']['requestBody']['content']['application/json'];
type SearchThesesResponse = operations['searchTheses']['responses'][200]['content']['application/json'];

type ThesisProposal = components['schemas']['ThesisProposal'];

type ThesisSearchResult = components['schemas']['ThesisSearchResult'];

export async function listTheses(client: ApiClient = defaultApiClient): Promise<ThesisSearchResult[]> {
  // if (USE_MOCK_THESIS) {
  //   return MOCK_THESES;
  // }

  return client.request<ThesisSearchResult[]>('/api/v1/theses', {
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
