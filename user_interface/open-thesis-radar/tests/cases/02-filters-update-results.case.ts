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

  test('degree type filtering keeps theses with unspecified degree visible', async ({ page }) => {
    await setupApiMocks(page, {
      theses: [
        {
          id: 1001,
          title: 'Robot Perception for Warehouse Navigation',
          chairId: 1,
          chairName: 'Chair of Robotics',
          degreeType: 'Master',
          researchArea: 'Robotics',
          status: 'OPEN',
          aiOverview: 'Explore perception methods for autonomous warehouse robots.',
          originalDescription: 'This thesis studies robot perception in warehouses.',
          semanticScore: null,
        },
        {
          id: 1004,
          title: 'Open Topic in Human-Centered Computing',
          chairId: 2,
          chairName: 'Chair of Distributed Systems',
          degreeType: null,
          researchArea: 'Cloud Computing',
          status: 'OPEN',
          aiOverview: 'Define a thesis topic together with the chair.',
          originalDescription: 'This thesis has no specified degree type yet.',
          semanticScore: null,
        },
      ],
    });
    await page.goto('/');

    await page.getByLabel('Degree Type filter search').click();
    await page.getByRole('button', { name: 'Master' }).click();

    await expect(page.getByRole('link', { name: /Robot Perception for Warehouse Navigation/i })).toBeVisible();
    await expect(page.getByRole('link', { name: /Open Topic in Human-Centered Computing/i })).toBeVisible();
  });
}
