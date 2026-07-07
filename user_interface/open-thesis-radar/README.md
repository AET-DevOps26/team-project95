# Open Thesis Radar frontend

## End-to-end tests

The frontend uses Playwright for browser workflow tests. The tests live in `tests/` and mock backend API responses, so a live backend is not required.

Run the tests locally with:

```bash
npm ci
npx playwright install chromium
npm run build
npm run test:client-side
```

For a visible browser while debugging, run:

```bash
npm run test:client-side:headed
```

To inspect the last HTML report, run:

```bash
npm run test:client-side:report
```

### Playwright configuration

The Playwright setup is defined in `playwright.config.ts`.

- `testDir: './tests'` tells Playwright to discover test files in the `tests/` directory.
- `fullyParallel: true` allows independent tests to run in parallel.
- `reporter` uses GitHub annotations plus an HTML report in CI, and a terminal list plus an HTML report locally.
- `use.baseURL` points tests to `http://127.0.0.1:4173`, so tests can navigate with paths like `page.goto('/')`.
- `use.trace: 'on-first-retry'` records a trace only when a failed test is retried, which keeps normal runs light while preserving debug information for failures.
- `webServer.command` starts the built Vite app with `npm run preview -- --host 127.0.0.1 --port 4173` before the tests run.
- `webServer.reuseExistingServer` reuses a local server during development, but starts a fresh server in CI.
- `projects` currently runs the suite in Chromium using Playwright's Desktop Chrome preset.

Because the tests mock `/api/v1/...` responses in Playwright, the preview server only needs to serve the frontend; no live backend is required.

### Test case coverage

1. **Home page renders thesis results**: verifies that the home page loads successfully, consumes mocked thesis and filter API responses, and displays the initial thesis result cards to the user.
2. **Filters update visible results**: verifies that selecting a frontend filter, such as `Degree Type -> Master`, updates the visible thesis list and hides theses that no longer match the selected filters.
3. **Filter dropdown search**: verifies that typing into a filter dropdown search field narrows the available dropdown options and keeps matching entries visible.
4. **Reset clears filters**: verifies that after applying filters, clicking `Reset all` clears the selected filter state and restores the full thesis result list.
5. **Natural-language search request**: verifies that submitting the semantic search form sends the expected request payload to `/api/v1/theses/search` and renders the mocked response returned by the API. This does not test backend semantic search quality.
6. **Detail page renders thesis data**: verifies that opening `/thesis/:id` loads mocked thesis detail data and renders the title, chair, degree type, research area, overview, and advisor information.
7. **Thesis card navigation**: verifies that clicking a thesis result card on the home page navigates the user to the corresponding detailed thesis page.
8. **Loading and error states**: verifies that the detail page shows a loading state while thesis data is pending and displays an error state when the thesis detail API request fails.

## React + TypeScript + Vite

This project uses Vite with React, TypeScript, and ESLint.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```
