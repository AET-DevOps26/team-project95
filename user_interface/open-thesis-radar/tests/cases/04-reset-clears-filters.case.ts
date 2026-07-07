import { expect, test } from '@playwright/test';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers the reset workflow test for selected filters.
// It ensures users can recover the complete result list after narrowing it down.
export function registerResetClearsFiltersTest() {
  test('reset all clears selected filters and restores all results', async ({ page }) => {
    await setupApiMocks(page);
    await page.goto('/');

    await page.getByLabel('Degree Type filter search').click();
    await page.getByRole('button', { name: 'Master' }).click();
    await expect(page.getByRole('link', { name: /Energy-Aware Cloud Scheduling/i })).not.toBeVisible();

    await page.getByRole('button', { name: 'Reset all' }).click();

    await expect(page.getByRole('link', { name: /Robot Perception for Warehouse Navigation/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Energy-Aware Cloud Scheduling/i })).toBeVisible();
  });
}
