# Video 3 — Why Your Selenium & Playwright Tests Are Flaky (5 Fixes That Work)

Each fix has a **FLAKY** version and a **FIXED** version, in both tools.
The FLAKY versions are switched off by default so the suite stays green. Turn them on to watch them fail.

| # | Fix | Practice page |
|---|---|---|
| 1 | Replace fixed sleeps with condition waits | the-internet `/dynamic_loading/2` |
| 2 | Use retrying assertions / explicit waits, not one-time reads | the-internet `/dynamic_loading/1` |
| 3 | Stable locators (`data-test`, role, text), not position-based CSS/XPath | saucedemo inventory (sorted Z→A) |
| 4 | Every test creates its own state; no order dependence | saucedemo cart |
| 5 | Wait for the state change that matters (enabled, not just clicked) | the-internet `/dynamic_controls` |

## Playwright (TypeScript)
```bash
cd playwright
npm install
npx playwright install --with-deps chromium
npx playwright test       # 5 FIXED tests run, 5 FLAKY tests skipped
npm run test:flaky        # also runs the FLAKY tests (expect failures)
```

## Selenium (Java 17 + Maven + TestNG)
```bash
cd selenium-java
mvn test                              # 5 FIXED tests
mvn test -Dsuite=testng-flaky.xml     # 5 FLAKY tests (expect failures)
mvn test -Dheadless=false             # watch the browser
```

## Quick checklist to paste into your team wiki
- [ ] No `Thread.sleep` / `waitForTimeout` in test code
- [ ] Assertions that retry (`await expect(...)`) or explicit `WebDriverWait`
- [ ] Locators tied to test ids or user-visible text, never to layout position
- [ ] Each test logs in / seeds its own data
- [ ] Wait for the *result* of an action (enabled, message shown, request finished)
- [ ] Implicit wait = 0 in Selenium when you use explicit waits

Full frameworks with these patterns built in: https://swtestprecision.gumroad.com

---

## Software Test Precision
- 📺 YouTube: https://www.youtube.com/@softwaretestprecision
- 🧰 Templates & starter frameworks: https://swtestprecision.gumroad.com

License: MIT (see `LICENSE`).
