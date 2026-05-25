import type { operations } from '../api';
import { defaultApiClient, type ApiClient } from './client';

type AvailableFiltersResponse =
  operations['getAvailableFilters']['responses'][200]['content']['application/json'];

export async function getAvailableFilters(
  client: ApiClient = defaultApiClient,
): Promise<AvailableFiltersResponse> {
  return client.request<AvailableFiltersResponse>('/api/v1/filters', {
    method: 'GET',
  });
}
