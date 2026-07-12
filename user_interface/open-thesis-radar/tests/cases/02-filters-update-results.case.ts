import { expect, test } from '@playwright/test';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers the client-side filter behavior test.
// This focuses on frontend-owned filtering rather than backend search quality.
export function registerFiltersUpdateResultsTest() {
  test('selecting a filter updates the visible thesis results', async ({ page }) => {
    await setupApiMocks(page);
    await page.goto('/');

    await page.getByLabel('Degree Type filter search').click();
    await page.getByRole('button', { name: 'Master' }).click();

    await expect(page.getByRole('link', { name: /Robot Perception for Warehouse Navigation/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Energy-Aware Cloud Scheduling/i })).not.toBeVisible();
  });
}
