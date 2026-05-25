import type { components, operations } from '../api';
import { defaultApiClient, type ApiClient } from './client';

type SearchThesesRequest = operations['searchTheses']['requestBody']['content']['application/json'];
type SearchThesesResponse = operations['searchTheses']['responses'][200]['content']['application/json'];
type ThesisProposal = components['schemas']['ThesisProposal'];

export async function searchTheses(
  request: SearchThesesRequest,
  client: ApiClient = defaultApiClient,
): Promise<SearchThesesResponse> {
  return client.request<SearchThesesResponse>('/api/v1/theses/search', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function getThesisById(
  thesisId: number,
  client: ApiClient = defaultApiClient,
): Promise<ThesisProposal> {
  return client.request<ThesisProposal>(`/api/v1/theses/${thesisId}`, {
    method: 'GET',
  });
}
