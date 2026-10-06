# User Stories, Backlog, and Sprint Plan

Covers **Task 2** (user stories, initial backlog, sprint plan) and
**Task 6** (the backlog as updated after the first review).

---

## Part 1 — Personas

| Persona | Description |
|---|---|
| **Dispatcher** | Plans shipments. Needs to record a delivery quickly and correctly. |
| **Logistics coordinator** | Watches the whole picture. Needs to see what is moving and confirm arrivals. |
| **Centre manager** | Runs one centre. Needs to know what stock is on hand. |

## Part 2 — User stories

Format: *As a … I want … so that …*

### US-01 — Record a delivery
**As a** dispatcher,
**I want** to record a delivery of an item and quantity,
**so that** the movement is documented instead of being written on paper.

- **Priority:** Must have
- **Acceptance criteria**
  1. Given the warehouse has stock, when I submit an item name and a positive
     quantity, then a delivery is created and shown in the delivery list.
  2. The delivery is assigned an identifier (`D1`, `D2`, …) that is unique
     within the session.
  3. The new delivery's status is `PENDING`.
  4. A success message names the identifier that was assigned.
  5. The summary counts increase by one.

- **Implemented:** yes — `tracker.createDelivery`, create form in `index.jsp`

### US-02 — Reject invalid input
**As a** dispatcher,
**I want** invalid input to be rejected with a readable explanation,
**so that** a mistake is obvious immediately instead of corrupting the data.

- **Priority:** Must have
- **Acceptance criteria**
  1. Blank item name → message, no delivery created.
  2. Non-numeric quantity → message, no delivery created.
  3. Quantity of zero or less → message, no delivery created.
  4. Quantity greater than available warehouse stock → message naming the item
     and the available quantity.
  5. In every case the page still renders and no stack trace is shown.

- **Implemented:** yes — validation block in `index.jsp`

### US-03 — Advance a delivery's status
**As a** logistics coordinator,
**I want** to advance a delivery from pending to in-transit to delivered,
**so that** the lifecycle of a shipment is visible.

- **Priority:** Must have
- **Acceptance criteria**
  1. Each row offers exactly one action button for its current status.
  2. Clicking it advances the delivery by exactly one step.
  3. A message confirms the new status.
  4. A delivery already `DELIVERED` cannot be advanced further.

- **Implemented:** yes — the `advance` action in `index.jsp`

### US-04 — Credit destination stock on arrival
**As a** centre manager,
**I want** stock to increase automatically when a delivery reaches me,
**so that** my inventory stays correct without manual correction.

- **Priority:** Must have — *this is the behaviour the whole application exists for*
- **Acceptance criteria**
  1. Marking a delivery `DELIVERED` increases the destination centre's stock for
     that item by the delivered quantity.
  2. Advancing to `IN_TRANSIT` does **not** change any stock.
  3. The new stock level is visible on the same screen.
  4. Marking the same delivery delivered twice does not double-credit — it is
     already the final status, so no further advance is offered.

- **Implemented:** yes — `updateDeliveryStatus` credits on `DELIVERED`

### US-05 — See live inventory
**As a** centre manager,
**I want** to see stock per centre and item,
**so that** I know what is on hand without asking someone.

- **Priority:** Should have
- **Acceptance criteria**
  1. Every centre is listed with its name and identifier.
  2. Every item a centre holds is listed with its quantity.
  3. The table reflects stock changes without a manual refresh action.

- **Implemented:** yes — `getStockForCenter` + inventory table

### US-06 — See delivery counts by status
**As a** logistics coordinator,
**I want** to see how many deliveries are in each status,
**so that** I can judge whether stock is moving.

- **Priority:** Should have
- **Acceptance criteria**
  1. Total, pending, in-transit, and delivered counts are shown.
  2. Counts reflect creations and status changes immediately.
  3. Counts total correctly.

- **Implemented:** yes — summary table

### US-07 — Search deliveries
**As a** logistics coordinator,
**I want** to filter the delivery list by id, item, or status,
**so that** I can find one shipment among many.

- **Priority:** Should have
- **Acceptance criteria**
  1. Search matches against delivery id, item name, and status.
  2. Matching is case-insensitive and substring-based.
  3. A term with no matches shows an empty list, not an error.
  4. A Clear control resets the filter.

- **Implemented:** yes — search filter in `index.jsp`

### US-08 — Automatic build and test on every commit
**As a** developer,
**I want** every push to `main` built and tested automatically,
**so that** I find out immediately when I have broken something.

- **Priority:** Must have
- **Acceptance criteria**
  1. A push to `main` triggers a build without manual action.
  2. Unit tests run on every build.
  3. Test results are published to the build.

- **Implemented:** yes — Jenkins job with SCM polling

### US-09 — Browser tests gate deployment
**As a** release owner,
**I want** the browser tests to run against the new build and to block
deployment if they fail,
**so that** a broken version never reaches the live application.

- **Priority:** Must have
- **Acceptance criteria**
  1. The new build is deployed to a staging context and the browser suite runs
     against it.
  2. If any test fails, the build is not promoted to the live context.
  3. A failure screenshot is archived with the build.
  4. The staging context is removed afterwards on every outcome.

- **Implemented:** yes — see `docs/CI-REPORT.md`

### US-21 — Add stock for an item
**As a** dispatcher,
**I want** to add stock to a center for any item,
**so that** I can distribute items beyond the ones the application was seeded with.

- **Priority:** Must have
- **Acceptance criteria**
  1. Given a center and an item name and quantity, the user can add stock.
  2. An item the center does not yet hold is created at that center.
  3. An item the center already holds accumulates rather than being replaced.
  4. The message reports the new total for that item.
  5. Adding stock creates no delivery and changes no delivery status.
  6. An item added this way becomes deliverable immediately.
  7. A blank item name, a non-numeric quantity, a non-positive quantity, and an
     unrecognized center are each rejected with a message.

- **Implemented:** yes — the *Add Stock* form, `addStock`, `addStockForNewItem` (Selenium)

---

## Part 3 — Initial backlog (before development)

Ordered by priority. MoSCoW: **M**ust, **S**hould, **C**ould, **W**on't.

| ID | Story | Priority | Status |
|---|---|---|---|
| US-01 | Record a delivery | M | Done |
| US-02 | Reject invalid input | M | Done |
| US-03 | Advance delivery status | M | Done |
| US-04 | Credit stock on arrival | M | Done |
| US-05 | See live inventory | S | Done |
| US-06 | See delivery counts by status | S | Done |
| US-07 | Search deliveries | S | Done |
| US-08 | Auto build and test on commit | M | Done |
| US-09 | Browser tests gate deployment | M | Done |
| US-10 | Persist data in a database | M | **Deferred** |
| US-11 | Multiple source/destination centres | C | **Deferred** |
| US-12 | User accounts and roles | W | **Deferred** |
| US-13 | Edit or cancel a delivery | C | **Deferred** |
| US-14 | Decrement source stock on dispatch | S | **Deferred** |
| US-15 | Export deliveries to CSV | C | **Deferred** |
| US-16 | Escape untrusted output | M | **Done** — closed alongside US-21, which made stock item names reachable |

US-21 (add stock) was **not** in the initial backlog. It was added after
development when the seeded single item proved to be the main thing limiting
real use. It is recorded in Part 5 rather than backdated into this list.

---

## Part 4 — Sprint plan

Three sprints over three weeks. Each sprint ends with a review.

### Sprint 1 — Core domain and MVP (Week 1)

**Goal:** a delivery can be created and tracked to arrival.

| Story | Priority |
|---|---|
| US-01 Record a delivery | M |
| US-02 Reject invalid input | M |
| US-03 Advance delivery status | M |
| US-04 Credit stock on arrival | M |

**Exit criteria**
- A delivery can be created, advanced twice, and destination stock increases
  exactly once, on arrival
- 7 unit tests pass, covering the domain rules including the negative cases
- Deliverable: working application

**Delivered:** yes

### Sprint 2 — Visibility and search (Week 2)

**Goal:** a coordinator can answer operational questions from one screen.

| Story | Priority |
|---|---|
| US-05 See live inventory | S |
| US-06 See delivery counts by status | S |
| US-07 Search deliveries | S |
| Tasks 4 and 5: README, CONTRIBUTING, issue and PR templates | — |

**Exit criteria**
- All three sections render and update without a manual refresh
- Search filters by id, item, and status; an unmatched term shows an empty list
- README explains how to build and run; CONTRIBUTING defines the branch workflow

**Delivered:** yes

### Sprint 3 — Quality gate and release safety (Week 3)

**Goal:** make a broken build impossible to release.

| Story | Priority |
|---|---|
| US-08 Auto build and test on commit | M |
| US-09 Browser tests gate deployment | M |
| Tasks 1, 2, 3, 6: problem statement, stories, SRS, backlog | — |

**Exit criteria**
- A push to `main` builds automatically
- 3 browser tests cover create, search, and status update
- A deliberately introduced defect fails the build and is **not** promoted
- The corrected defect then builds green and is promoted

**Delivered:** yes. The deliberate-defect demonstration is in `docs/CI-REPORT.md`.

---

## Part 5 — Updated backlog (Task 6)

This is the backlog as it stands **after the first review**, not the initial
list. It reflects what was actually built, what the testing exposed, and what
was deliberately deferred.

### 5.1 Completed

| ID | Story | Priority | Notes |
|---|---|---|---|
| US-01 | Record a delivery | M | |
| US-02 | Reject invalid input | M | Four distinct validation messages |
| US-03 | Advance delivery status | M | |
| US-04 | Credit stock on arrival | M | Core value of the application |
| US-05 | See live inventory | S | |
| US-06 | See delivery counts by status | S | |
| US-07 | Search deliveries | S | |
| US-08 | Auto build and test on commit | M | SCM poll every 2 min |
| US-09 | Browser tests gate deployment | M | Staging context, promote on pass |
| US-21 | Add stock for an item | M | Added after review. Closes the limitation that only the seeded item could be distributed |
| US-16 | Escape untrusted output | M | Closed by the same change. The Add Stock form made the stock item name attacker-reachable, so it stopped being theoretical |

### 5.2 New items discovered during development

These were **not** in the initial backlog. They were found by writing the tests
and by running the pipeline, which is the point of a review.

| ID | Item | Priority | Why it appeared | Status |
|---|---|---|---|---|
| US-17 | Wait for navigation commit before asserting | M | The suite was flaky: assertions ran against the page about to be replaced. Three separate causes, all in `clickAndWaitForReload`. | Done |
| US-18 | Tolerate Chrome's transient discarded-node error | M | Jenkins build #8 failed on `Node with given id does not belong to the document`, raised from inside `stalenessOf`. A release gate that fails for the wrong reason is worse than no gate. | Done |
| US-19 | Poll the staging URL instead of a fixed sleep | M | Tomcat took ~12s to deploy; the old `sleep 8` was too tight. | Done |
| US-20 | Capture a screenshot on test failure | M | A CI failure was not diagnosable without local reproduction. | Done |

### 5.3 Deferred, with reasons

| ID | Item | Priority | Why deferred |
|---|---|---|---|
| US-10 | Persist data in a database | M | Session state is sufficient to demonstrate the workflow. Highest-value next item. |
| US-14 | Decrement source stock on dispatch | S | Dispatch is planned rather than confirmed. Crediting only on arrival keeps the stock figure honest. Needs a product decision first. |
| US-11 | Multiple source/destination centres | C | Requires the persistence work first. |
| US-13 | Edit or cancel a delivery | C | Needs an audit trail to be trustworthy. |
| US-15 | Export deliveries to CSV | C | Not needed for the workflow. |
| US-12 | User accounts and roles | W | Single trusted user assumed. |

### 5.4 Recommended next sprint

Ordered by what unblocks the most:

1. **US-10 Persistence** — session state means a Tomcat restart loses all data.
   This is the single biggest gap between a demo and something usable.
2. **US-14 Source stock on dispatch** — needs a product decision, then a test.
3. **US-11 Multiple centres** — depends on 1 and 2.
4. **US-12 Auth** — depends on 1.

US-16 (escape untrusted output) was the previous number 2 and is now closed.

### 5.5 Risks carried forward

| Risk | Impact | Mitigation |
|---|---|---|
| Session-scoped state lost on restart | All deliveries vanish | US-10 |
| Reflected XSS | **Closed** — every expression on the page is escaped | US-16, `userSuppliedOutputIsEscaped` |
| Single JVM state | Cannot run two instances | Resolved by US-10 |
