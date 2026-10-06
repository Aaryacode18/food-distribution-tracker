# Selenium Test Plan

## Scope

End-to-end coverage of the three critical user journeys through a real browser
against the deployed application. These tests complement, and do not replace,
the unit tests in `FoodDistributionTrackerTest`.

## Covered journeys

| # | Journey | Test method | What it proves |
|---|---------|-------------|----------------|
| 1 | Create a delivery | `createDelivery` | A dispatcher can record a delivery, sees the success message, the new row appears in the status table, and the summary counters increment. |
| 2 | Search and filter deliveries | `searchAndFilterDeliveries` | Search narrows the table by item name, by status, and by delivery id, and returns zero rows for a term with no match. |
| 3 | Update status and see inventory change | `updateDeliveryStatus` | A delivery moves `PENDING → IN_TRANSIT → DELIVERED`, and the destination center's C2 stock is credited only on `DELIVERED`. |
| 4 | Add stock for a new item | `addStockForNewItem` | An item the warehouse has never held can be stocked, is listed with the quantity added, is not confused with a delivery, and becomes deliverable so it can reach the destination. |
| 5 | Escapes user-supplied output | `userSuppliedOutputIsEscaped` | A `<script>` payload passed as the search term, the delivery item name and the stock item name reaches the response escaped in all three cases, and the search box still displays the original text. |

## Out of scope

- Cross-browser matrix. The suite runs in headless Chrome only.
- Validation messages. Asserting on exact rejection wording would couple the tests
  to the strings, so the four rejection paths are verified by hand instead. See
  `docs/srs.md` §6.4.
- Load and performance testing. Security testing is deliberately narrow: only
  output escaping is covered, because that is the one finding US-16 was open
  for. Authentication, CSRF and injection testing need features this application
  does not have yet (US-12).
- Negative paths covered at the service layer rather than the UI, such as an
  insufficient-stock delivery.

## Environment

- Application under test: `http://localhost:8081/food-distribution-tracker/`
- Override with `-Dapp.url=<url>`.
- Browser: headless Chrome, window size 1400x1000.
- Driver: resolved automatically by Selenium Manager. No manual `chromedriver`
  installation or version pinning is required.
- The application keeps all state in `HttpSession`, and each test starts a
  fresh browser, so every test begins from a clean session. `searchAndFilterDeliveries`
  asserts this precondition explicitly.

## Running

```bash
# Unit tests only. The Selenium suite is excluded from the default run.
mvn test

# Browser tests only.
mvn test -Pselenium

# Against a different deployment.
mvn test -Pselenium -Dapp.url=http://localhost:9090/food-distribution-tracker/
```

The `selenium` profile is intentionally separate so that a plain `mvn test` stays
fast and does not require a browser or a running Tomcat.

## Failure diagnostics

A JUnit `TestWatcher` rule captures a PNG screenshot of the browser at the
moment of failure into `target/selenium-screenshots/<testMethod>.png` and logs
its path. This is what makes a CI failure diagnosable without re-running the
suite locally.

## Determinism notes

Every action in the application is a full form `POST` or `GET`, so each
interaction replaces the whole document. Two classes of flake were found and
fixed while building this suite:

- Waiting on an element that is present on every page (the search box, the
  message div) or on text that is already there (a search term that was just
  typed, a status message that already says "updated to") returns immediately
  and lets assertions run against the page that is about to be replaced.
  The helpers therefore capture an `h1` element and wait for it to go stale,
  which only happens once the old document has actually been discarded.
- Chrome's DevTools protocol can transiently reject a click with
  "Node with given id does not belong to the document" while the previous
  document is being torn down. `clickAndWaitForReload` retries the click a
  bounded number of times.

Delivery rows are rendered from a `HashMap`, so their order is not guaranteed.
Tests select rows by delivery id rather than by position.

A center has one inventory row per item. Once stock for several items exists,
asserting on a center's *first* row silently depends on sort order, so
`inventoryText` joins every row belonging to that center, and `inventoryItemRow`
pins an assertion to one specific item. Without this, adding a second item made
two unrelated tests fail.
