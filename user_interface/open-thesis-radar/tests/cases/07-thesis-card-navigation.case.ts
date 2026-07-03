import { expect, test } from '@playwright/test';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers the result-card navigation test.
// It confirms that a user can move from search results to the detailed thesis route.
export function registerThesisCardNavigationTest() {
  test('clicking a thesis result navigates to the detailed thesis page', async ({ page }) => {
    await setupApiMocks(page);
    await page.goto('/');

    await page.getByRole('link', { name: /Robot Perception for Warehouse Navigation/i }).click();

    await expect(page).toHaveURL(/\/thesis\/1001$/);
    await expect(page.getByRole('heading', { name: 'Robot Perception for Warehouse Navigation' })).toBeVisible();
    await expect(page.getByText('Original Description')).toBeVisible();
  });
}
