import { expect, test } from '@playwright/test';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers the filter dropdown search test.
// It confirms that option matching happens in the browser as the user types.
export function registerFilterDropdownSearchTest() {
  test('typing in a filter dropdown keeps the closest matching options visible', async ({ page }) => {
    await setupApiMocks(page);
    await page.goto('/');

    await page.getByLabel('Research Area filter search').fill('robot');

    await expect(page.getByRole('button', { name: 'Robotics' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cloud Computing' })).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Artificial Intelligence' })).not.toBeVisible();
  });
}
