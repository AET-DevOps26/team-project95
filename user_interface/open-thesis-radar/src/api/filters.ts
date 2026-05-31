import type { operations } from '../api';
import { USE_MOCK_THESIS } from '../config';
import { MOCK_FILTERS } from '../mocks/theses';
import { defaultApiClient, type ApiClient } from './client';

type AvailableFiltersResponse =
  operations['getAvailableFilters']['responses'][200]['content']['application/json'];

export async function getAvailableFilters(
  client: ApiClient = defaultApiClient,
): Promise<AvailableFiltersResponse> {
  if (USE_MOCK_THESIS) {
    return MOCK_FILTERS;
  }

  return client.request<AvailableFiltersResponse>('/api/v1/filters', {
    method: 'GET',
  });
}
