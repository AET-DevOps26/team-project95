import { expect, test } from '@playwright/test';
import { semanticSearchThesis } from '../fixtures';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers the natural-language search wiring test.
// It verifies request shape and result rendering without judging backend semantic relevance.
export function registerNaturalLanguageSearchTest() {
  test('natural-language search submits the expected request and displays returned results', async ({ page }) => {
    let searchPayload: unknown;
    await setupApiMocks(page, {
      searchResults: [semanticSearchThesis],
      onSearchRequest: (payload) => {
        searchPayload = payload;
      },
    });
    await page.goto('/');

    await page.getByPlaceholder('Describe what you are looking for...').fill('explainable healthcare ai');
    await page.locator('form').getByRole('button', { name: 'Search' }).click();

    await expect(page.getByRole('link', { name: /Explainable AI for Medical Decision Support/i })).toBeVisible();
    expect(searchPayload).toEqual({
      naturalLanguageQuery: 'explainable healthcare ai',
      filters: {
        chairIds: [],
        degreeTypes: [],
        researchAreas: [],
        tags: [],
        status: 'OPEN',
      },
      page: 0,
      size: 50,
    });
  });
}
