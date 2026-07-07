import type { Page, Route } from '@playwright/test';
import { mockFilters, mockTheses, mockThesisDetail, semanticSearchThesis } from '../fixtures';

type ApiMockOptions = {
  theses?: unknown[];
  filters?: unknown;
  searchResults?: unknown[];
  thesisDetail?: unknown;
  thesisListError?: string;
  thesisDetailError?: string;
  onSearchRequest?: (payload: unknown) => void;
};

// Sends deterministic JSON responses for mocked backend routes.
// Keeping this helper central avoids repeating response headers in every route mock.
async function fulfillJson(route: Route, json: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(json),
  });
}

// Registers Playwright network mocks for the frontend API endpoints.
// Tests can override individual responses while keeping the default happy path concise.
export async function setupApiMocks(page: Page, options: ApiMockOptions = {}) {
  const theses = options.theses ?? mockTheses;
  const filters = options.filters ?? mockFilters;
  const searchResults = options.searchResults ?? [semanticSearchThesis];
  const thesisDetail = options.thesisDetail ?? mockThesisDetail;

  await page.route('**/api/v1/filters', async (route) => {
    await fulfillJson(route, filters);
  });

  await page.route('**/api/v1/theses/search', async (route) => {
    options.onSearchRequest?.(route.request().postDataJSON());

    await fulfillJson(route, {
      items: searchResults,
      page: 0,
      size: 50,
      totalElements: searchResults.length,
    });
  });

  await page.route(/\/api\/v1\/theses\/\d+$/, async (route) => {
    if (options.thesisDetailError) {
      await fulfillJson(route, { message: options.thesisDetailError }, 500);
      return;
    }

    await fulfillJson(route, thesisDetail);
  });

  await page.route('**/api/v1/theses', async (route) => {
    if (options.thesisListError) {
      await fulfillJson(route, { message: options.thesisListError }, 500);
      return;
    }

    await fulfillJson(route, theses);
  });
}
