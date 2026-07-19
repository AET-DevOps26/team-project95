import { expect, test } from '@playwright/test';
import { setupApiMocks } from '../helpers/apiMocks';

// Registers loading and error state tests for the detail page.
// These cases verify that failed or pending API responses produce useful user feedback.
export function registerLoadingAndErrorStatesTest() {
  test('detail page shows a loading state while thesis data is pending', async ({ page }) => {
    await setupApiMocks(page);
    await page.route(/\/api\/v1\/theses\/1001$/, async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 300));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1001,
          title: 'Robot Perception for Warehouse Navigation',
          chairId: 1,
          chairName: 'Chair of Robotics',
          degreeType: 'Master',
          researchArea: 'Robotics',
          status: 'OPEN',
          aiOverview: 'Explore perception methods for autonomous warehouse robots.',
          originalDescription: 'This thesis studies robot perception in warehouses.',
          advisors: [],
          sourceUrl: 'https://example.edu/theses/robot-perception',
          lastSeenAt: '2026-06-15T10:00:00Z',
        }),
      });
    });

    await page.goto('/thesis/1001');

    await expect(page.getByRole('heading', { name: 'Loading thesis details' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Robot Perception for Warehouse Navigation' })).toBeVisible();
  });

  test('detail page shows an error state when thesis loading fails', async ({ page }) => {
    await setupApiMocks(page, { thesisDetailError: 'Thesis service unavailable' });

    await page.goto('/thesis/1001');

    await expect(page.getByRole('heading', { name: 'Could not load thesis' })).toBeVisible();
    await expect(page.getByText('Thesis service unavailable')).toBeVisible();
  });
}
