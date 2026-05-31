import type { operations } from '../api';
import { defaultApiClient, type ApiClient } from './client';

type ChairListResponse = operations['listChairs']['responses'][200]['content']['application/json'];

export async function listChairs(client: ApiClient = defaultApiClient): Promise<ChairListResponse> {
  return client.request<ChairListResponse>('/api/v1/chairs', {
    method: 'GET',
  });
}
