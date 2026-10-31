import { test, expect } from '@playwright/test';

/**
 * Video 3: Why your tests are flaky — 5 fixes that work.
 *
 * Every fix has a FLAKY version and a FIXED version.
 * FLAKY versions are skipped by default. To watch them misbehave:
 *   npm run test:flaky
 */
const showFlaky = !!process.env.SHOW_FLAKY;
const INTERNET = 'https://the-internet.herokuapp.com';
const SAUCE = 'https://www.saucedemo.com';

async function loginToSauce(page: import('@playwright/test').Page) {
  await page.goto(SAUCE);
  await page.locator('[data-test="username"]').fill('standard_user');
  await page.locator('[data-test="password"]').fill('secret_sauce');
  await page.locator('[data-test="login-button"]').click();
  await expect(page).toHaveURL(/inventory/);
}

// ---------------------------------------------------------------------------
// Fix 1: Replace fixed sleeps with waiting for a condition
// ---------------------------------------------------------------------------
test.describe('Fix 1 - no fixed sleeps', () => {
  test('FLAKY: guesses how long loading takes', async ({ page }) => {
    test.skip(!showFlaky, 'flaky example - run with SHOW_FLAKY=1');
    await page.goto(`${INTERNET}/dynamic_loading/2`);
    await page.getByRole('button', { name: 'Start' }).click();
    await page.waitForTimeout(2000); // the server sometimes needs 5+ seconds
    const text = await page.locator('#finish').textContent({ timeout: 100 });
    expect(text).toContain('Hello World!');
  });

  test('FIXED: waits for the result to appear', async ({ page }) => {
    await page.goto(`${INTERNET}/dynamic_loading/2`);
    await page.getByRole('button', { name: 'Start' }).click();
    await expect(page.locator('#finish')).toHaveText('Hello World!', { timeout: 15_000 });
  });
});

// ---------------------------------------------------------------------------
// Fix 2: Use retrying (web-first) assertions, not one-time reads
// ---------------------------------------------------------------------------
test.describe('Fix 2 - retrying assertions', () => {
  test('FLAKY: reads visibility once, right after the click', async ({ page }) => {
    test.skip(!showFlaky, 'flaky example - run with SHOW_FLAKY=1');
    await page.goto(`${INTERNET}/dynamic_loading/1`);
    await page.getByRole('button', { name: 'Start' }).click();
    // isVisible() returns immediately; it does not wait.
    expect(await page.locator('#finish').isVisible()).toBe(true);
  });

  test('FIXED: expect(...).toBeVisible() retries until it passes or times out', async ({ page }) => {
    await page.goto(`${INTERNET}/dynamic_loading/1`);
    await page.getByRole('button', { name: 'Start' }).click();
    await expect(page.locator('#finish')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('#finish')).toHaveText('Hello World!');
  });
});

// ---------------------------------------------------------------------------
// Fix 3: Stable locators instead of layout-dependent CSS
// ---------------------------------------------------------------------------
test.describe('Fix 3 - stable locators', () => {
  test('FLAKY: position-based CSS breaks when the layout or sort order changes', async ({ page }) => {
    test.skip(!showFlaky, 'flaky example - run with SHOW_FLAKY=1');
    await loginToSauce(page);
    await page.locator('[data-test="product-sort-container"]').selectOption('za');
    // "the first item's button" is no longer the backpack after sorting.
    await page.locator('.inventory_list > div:nth-child(1) button').click();
    await page.locator('.shopping_cart_link').click();
    await expect(page.locator('[data-test="inventory-item-name"]')).toHaveText(['Sauce Labs Backpack'], {
      timeout: 2000,
    });
  });

  test('FIXED: locate by test id or by what the user sees', async ({ page }) => {
    await loginToSauce(page);
    await page.locator('[data-test="product-sort-container"]').selectOption('za');
    const backpack = page.locator('[data-test="inventory-item"]').filter({ hasText: 'Sauce Labs Backpack' });
    await backpack.getByRole('button', { name: 'Add to cart' }).click();
    await page.locator('[data-test="shopping-cart-link"]').click();
    await expect(page.locator('[data-test="inventory-item-name"]')).toHaveText(['Sauce Labs Backpack']);
  });
});

// ---------------------------------------------------------------------------
// Fix 4: Every test sets up its own state (no order dependence)
// ---------------------------------------------------------------------------
test.describe('Fix 4 - independent tests', () => {
  test('FLAKY: assumes a previous test already logged in and filled the cart', async ({ page }) => {
    test.skip(!showFlaky, 'flaky example - run with SHOW_FLAKY=1');
    // Passes only if run after another test in the same browser session. Each Playwright test gets
    // a fresh context, so this fails - which is exactly the point: never rely on test order.
    await page.goto(`${SAUCE}/cart.html`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('[data-test="inventory-item"]')).toHaveCount(1, { timeout: 2000 });
  });

  test('FIXED: test creates the state it needs', async ({ page }) => {
    await loginToSauce(page);
    await page.locator('[data-test="add-to-cart-sauce-labs-bike-light"]').click();
    await page.locator('[data-test="shopping-cart-link"]').click();
    await expect(page.locator('[data-test="inventory-item"]')).toHaveCount(1);
  });
});

// ---------------------------------------------------------------------------
// Fix 5: Wait for the state change you actually care about
// ---------------------------------------------------------------------------
test.describe('Fix 5 - wait for the real state change', () => {
  test('FLAKY: types as soon as the button is clicked', async ({ page }) => {
    test.skip(!showFlaky, 'flaky example - run with SHOW_FLAKY=1');
    await page.goto(`${INTERNET}/dynamic_controls`);
    await page.locator('#input-example button').click();
    // The input is still disabled for a few seconds; force skips the actionability checks.
    await page.locator('#input-example input').fill('hello', { force: true, timeout: 1000 });
    await expect(page.locator('#input-example input')).toHaveValue('hello', { timeout: 1000 });
  });

  test('FIXED: wait until the input is enabled, then type', async ({ page }) => {
    await page.goto(`${INTERNET}/dynamic_controls`);
    await page.locator('#input-example button').click();
    await expect(page.locator('#message')).toHaveText("It's enabled!", { timeout: 15_000 });
    const input = page.locator('#input-example input');
    await expect(input).toBeEnabled();
    await input.fill('hello');
    await expect(input).toHaveValue('hello');
  });
});
