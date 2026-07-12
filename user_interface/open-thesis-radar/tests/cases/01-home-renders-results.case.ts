import { expect, test } from '@playwright/test';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers the home page happy-path test for mocked thesis list rendering.
// This verifies that the app can load its initial API data and expose results to users.
export function registerHomeRendersResultsTest() {
  test('home page renders thesis results from mocked API data', async ({ page }) => {
    await setupApiMocks(page);

    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Search Open Theses', exact: true })).toBeVisible();
    await expect(page.getByText('2 thesis proposals available')).toBeVisible();
    await expect(page.getByRole('link', { name: /Robot Perception for Warehouse Navigation/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Energy-Aware Cloud Scheduling/i })).toBeVisible();
  });
}
