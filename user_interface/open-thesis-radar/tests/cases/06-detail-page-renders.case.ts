import { expect, test } from '@playwright/test';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers the detail page rendering test for a mocked thesis response.
// This protects the user flow that opens a proposal and reads its metadata.
export function registerDetailPageRendersTest() {
  test('detail page loads a thesis by id and renders its details', async ({ page }) => {
    await setupApiMocks(page);

    await page.goto('/thesis/1001');

    await expect(page.getByRole('heading', { name: 'Robot Perception for Warehouse Navigation' })).toBeVisible();
    await expect(page.getByText('Offered by Chair of Robotics')).toBeVisible();
    await expect(page.getByText('Master').first()).toBeVisible();
    await expect(page.getByText('Robotics').first()).toBeVisible();
    await expect(page.getByText('Explore perception methods for autonomous warehouse robots.')).toBeVisible();
    await expect(page.getByText('Dr. Ada Lovelace')).toBeVisible();
  });
}
