# Software Requirements Specification — Food Distribution Tracker

**Version:** 1.0
**Status:** Implemented. Every requirement below is marked with its verification.

---

## 1. Introduction

### 1.1 Purpose

This document specifies the functional and non-functional requirements for the
Food Distribution Tracker, a small web application that records food
deliveries between distribution centres and updates inventory when a delivery
is confirmed as having arrived.

### 1.2 Scope

The application serves three users — a dispatcher, a logistics coordinator, and
a centre manager — who are treated as one trusted role. It covers recording a
delivery, tracking it to arrival, and reporting stock.

Deliberately excluded: database persistence, authentication, editing or
cancelling deliveries, and choosing the source/destination route. These are
recorded as limitations in `docs/user-stories-and-backlog.md`.

### 1.3 Definitions

| Term | Meaning |
|---|---|
| **Distribution centre** | A location holding food stock. Identified by an id and a display name. |
| **Delivery** | A shipment of a quantity of one item between two centres. |
| **Delivery status** | Where a delivery is in its lifecycle: `PENDING`, `IN_TRANSIT`, or `DELIVERED`. |
| **Warehouse (C1)** | Central Warehouse. The source. Seeded with 500 kg of Rice. |
| **Downtown Centre (C2)** | The destination. |

### 1.4 Reference

`FoodDistributionTracker` is the domain service. `index.jsp` is the only page
and contains both the request handling and the markup.

---

## 2. Overall description

### 2.1 Product perspective

A single-page web application, deployed as a WAR to Apache Tomcat. There is no
servlet, no `web.xml`, and no DAO layer. All state is held in the HTTP session,
so each browser session gets its own independent copy of the data.

### 2.2 User characteristics

All users are trusted staff. No authentication is required in this version.

### 2.3 Constraints

| Constraint | Effect |
|---|---|
| Java 21 | Compilation target. Builds on JDK 22 via `--release 21`. |
| Maven | The supported build tool. A `build.gradle` is present but requests a Java 21 toolchain and fails on newer JDKs; it is not the supported path. |
| Apache Tomcat | Runtime container, deploying the WAR. |
| Session-scoped state | No data survives a Tomcat restart. |
| Fixed route C1 → C2 | Source and destination are hard-coded. |

### 2.4 Assumptions

- One dispatcher per browser session; concurrent coordinators in separate
  sessions do not see each other's data.
- A centre holds stock per item name as a free-form string.
- Dispatch is planned, not confirmed, so source stock is credited on arrival
  rather than debited on dispatch.

---

## 3. Functional requirements

### 3.1 Stock management

| ID | Requirement | Verified by |
|---|---|---|
| FR-1.1 | The system shall record a stock quantity for a given centre and item. | `testAddAndGetStock` |
| FR-1.2 | Adding stock for a centre and item that already holds stock shall accumulate rather than replace. | `testAddStockAccumulates` |
| FR-1.3 | The system shall report the stock level for a centre and item. | `testAddAndGetStock` |
| FR-1.4 | The system shall report all stock held by one centre. | Live inventory table |
| FR-1.5 | A centre and item with no recorded stock shall report a level of zero. | `testAddAndGetStock` |
| FR-1.6 | On first use in a session, the system shall seed Central Warehouse (C1) with 500 kg of Rice. | First page load |

### 3.2 Delivery creation

| ID | Requirement | Verified by |
|---|---|---|
| FR-2.1 | The system shall allow a user to create a delivery given an item name and quantity. | `createDelivery` (Selenium) |
| FR-2.2 | The system shall assign each delivery an identifier unique within the session, of the form `D<n>`, starting at `D1`. | `testCreateDeliveryStartsAsPending` |
| FR-2.3 | A newly created delivery shall have status `PENDING`. | `testCreateDeliveryStartsAsPending` |
| FR-2.4 | The system shall record the source centre, destination centre, item name, and quantity against the delivery. | `testCreateDeliveryStartsAsPending` |
| FR-2.5 | The source shall be Central Warehouse (C1) and the destination Downtown Centre (C2). | `index.jsp` |
| FR-2.6 | After a successful creation the system shall display a message naming the assigned identifier. | `createDelivery` (Selenium) |
| FR-2.7 | After a successful creation the delivery shall appear in the delivery list. | `createDelivery` (Selenium) |
| FR-2.8 | After a successful creation the summary counts shall increase by one. | `createDelivery` (Selenium) |

### 3.3 Input validation

| ID | Requirement | Verified by |
|---|---|---|
| FR-3.1 | If the item name is blank, the system shall reject the input and display "Item name is required." | `index.jsp` |
| FR-3.2 | If the quantity is not an integer, the system shall reject the input and display "Invalid quantity. Please enter a valid number." | `index.jsp` |
| FR-3.3 | If the quantity is zero or negative, the system shall reject the input and display "Quantity must be greater than 0." | `index.jsp` |
| FR-3.4 | If the quantity exceeds the warehouse stock for that item, the system shall reject the input and display a message naming the item and the available quantity. | `index.jsp` |
| FR-3.5 | In every rejection case no delivery shall be created and the page shall still render. | `index.jsp` |
| FR-3.6 | The system shall trim leading and trailing whitespace from the item name before use. | `index.jsp` |

### 3.4 Status transitions

| ID | Requirement | Verified by |
|---|---|---|
| FR-4.1 | A delivery shall move `PENDING → IN_TRANSIT → DELIVERED`, one step per user action. | `testUpdateDeliveryStatus` |
| FR-4.2 | The system shall offer exactly one action per row, appropriate to its current status. | `updateDeliveryStatus` (Selenium) |
| FR-4.3 | On a successful transition the system shall display a message naming the delivery and its new status. | `updateDeliveryStatus` (Selenium) |
| FR-4.4 | A delivery already at `DELIVERED` shall not be advanced further, and the system shall say so. | `index.jsp` |
| FR-4.5 | An advance request naming a delivery that does not exist shall display a "not found" message and change nothing. | `index.jsp` |
| FR-4.6 | Requesting a status update for an unknown delivery identifier shall raise an error at the service layer. | `testUpdateNonexistentDeliveryThrows` |

### 3.5 Stock crediting on arrival

| ID | Requirement | Verified by |
|---|---|---|
| FR-5.1 | When a delivery's status becomes `DELIVERED`, the system shall add the delivered quantity to the destination centre's stock for that item. | `testDeliveredUpdatesDestinationStock` |
| FR-5.2 | Advancing to `IN_TRANSIT` shall not change any stock level. | `testUpdateDeliveryStatus` |
| FR-5.3 | Credited stock shall be visible without a manual refresh action. | `updateDeliveryStatus` (Selenium) |
| FR-5.4 | The system shall not debit the source centre on dispatch. | Deliberate design decision; see §6.1 |

### 3.6 Reporting

| ID | Requirement | Verified by |
|---|---|---|
| FR-6.1 | The system shall display total, pending, in-transit, and delivered counts. | `createDelivery` (Selenium) |
| FR-6.2 | Counts shall update immediately after a creation or transition. | `updateDeliveryStatus` (Selenium) |
| FR-6.3 | The sum of the three status counts shall equal the total. | `createDelivery` (Selenium) |
| FR-6.4 | The system shall list every centre with its identifier and display name. | Live inventory table |
| FR-6.5 | The system shall list, for each centre, every item held and its quantity. | Live inventory table |

### 3.7 Search and filtering

| ID | Requirement | Verified by |
|---|---|---|
| FR-7.1 | The system shall filter the delivery list by a search term. | `searchAndFilterDeliveries` (Selenium) |
| FR-7.2 | The term shall be matched against delivery id, item name, and status. | `searchAndFilterDeliveries` (Selenium) |
| FR-7.3 | Matching shall be case-insensitive and substring-based. | `searchAndFilterDeliveries` (Selenium) |
| FR-7.4 | A term matching no delivery shall show an empty list and no error. | `searchAndFilterDeliveries` (Selenium) |
| FR-7.5 | The search field shall be pre-filled with the term after filtering. | `searchAndFilterDeliveries` (Selenium) |
| FR-7.6 | A Clear control shall remove the filter. | `index.jsp` |

### 3.8 Delivery listing

| ID | Requirement | Verified by |
|---|---|---|
| FR-8.1 | The delivery list shall show id, source, destination, item, quantity, status, and available action. | Delivery status table |
| FR-8.2 | The list shall include a delivery if either its source or destination is C1. | `testGetDeliveriesForCenter` |
| FR-8.3 | The list shall exclude deliveries unrelated to C1. | `testGetDeliveriesForCenter` |

### 3.9 Build, test, and deployment

| ID | Requirement | Verified by |
|---|---|---|
| FR-9.1 | A commit to `main` shall trigger a build without manual action. | Jenkins builds #3–#9 |
| FR-9.2 | Unit tests shall run on every build. | Jenkins build #9 |
| FR-9.3 | The application shall be deployed as a WAR by the pipeline. | Jenkins build #9 |
| FR-9.4 | Browser tests shall run against the built artifact. | Jenkins build #9, 3 tests |
| FR-9.5 | If any test fails, the build shall not be promoted to the live application. | Jenkins build #7, `Deploy` and `Verify` skipped |
| FR-9.6 | Test results shall be published to the build. | `junit` step |
| FR-9.7 | A screenshot shall be captured on browser test failure and archived. | Build #7 artifact |
| FR-9.8 | Build parameters shall be configurable without editing the pipeline. | 8 job parameters |

---

## 4. Non-functional requirements

### 4.1 Usability

| ID | Requirement | Status |
|---|---|---|
| NFR-1.1 | All actions shall be reachable from one screen without navigation. | Met — single page |
| NFR-1.2 | Validation failures shall be shown in plain language, not technical jargon. | Met |
| NFR-1.3 | The application shall not require a manual refresh to reflect a change. | Met — full page reload per action |

### 4.2 Reliability and correctness

| ID | Requirement | Status |
|---|---|---|
| NFR-2.1 | Destination stock shall be credited exactly once per delivery. | Met — crediting only on entry to `DELIVERED`, and no action is offered past it |
| NFR-2.2 | Invalid input shall never produce a partially created delivery. | Met |
| NFR-2.3 | A failing test shall prevent promotion of the build. | Met and demonstrated |
| NFR-2.4 | The browser suite shall be free of flakiness. | Met — three root causes found and fixed; see §5.3 |

### 4.3 Performance

| ID | Requirement | Status |
|---|---|---|
| NFR-3.1 | The page shall render in under 1 second on the local network. | Met — all data in memory |
| NFR-3.2 | A full build including tests shall complete in under 5 minutes. | Met — 43s for a full green build (Jenkins #9); 28s when a test fails early (#7) |

### 4.4 Maintainability

| ID | Requirement | Status |
|---|---|---|
| NFR-4.1 | Business rules shall live in the domain service, not in the page. | Met — `FoodDistributionTracker`; validation is the exception, see §6.2 |
| NFR-4.2 | The build shall be reproducible from a clean checkout with one command. | Met |
| NFR-4.3 | Browser tests shall be isolated from the default `mvn test` run. | Met — separate `selenium` profile |
| NFR-4.4 | No driver shall need to be installed by hand. | Met — Selenium Manager |

### 4.5 Security

| ID | Requirement | Status |
|---|---|---|
| NFR-5.1 | Untrusted output shall be HTML-escaped. | **Not met** — see §6.3 |
| NFR-5.2 | Secrets shall not be committed to the repository. | Met |
| NFR-5.3 | The session identifier shall be managed by the container. | Met — `HttpSession` |

### 4.6 Maintainability of the build

| ID | Requirement | Status |
|---|---|---|
| NFR-6.1 | A failed test shall be diagnosable from the build alone. | Met — screenshot archived |
| NFR-6.2 | The pipeline shall be version-controlled with the code. | Met — `Jenkinsfile` in the repository |

---

## 5. Verification

### 5.1 Automated test coverage

| Suite | Count | Command | Scope |
|---|---|---|---|
| Unit | 7 | `mvn test` | `FoodDistributionTracker` domain rules |
| Browser | 3 | `mvn test -Pselenium` | Create, search/filter, status update and stock crediting |

### 5.2 Requirement coverage by test

Unit tests, in declaration order:

| # | Test | Covers |
|---|---|---|
| 1 | `testAddAndGetStock` | FR-1.1, FR-1.3, FR-1.5 |
| 2 | `testAddStockAccumulates` | FR-1.2 |
| 3 | `testCreateDeliveryStartsAsPending` | FR-2.2, FR-2.3, FR-2.4 |
| 4 | `testUpdateDeliveryStatus` | FR-4.1, FR-5.2 |
| 5 | `testDeliveredUpdatesDestinationStock` | FR-5.1 |
| 6 | `testGetDeliveriesForCenter` | FR-8.2, FR-8.3 |
| 7 | `testUpdateNonexistentDeliveryThrows` | FR-4.6 |

| Requirement group | Covered by |
|---|---|
| FR-1.1 – FR-1.6 | Unit 1, 2; FR-1.6 verified by first page load |
| FR-2.1 – FR-2.8 | Browser `createDelivery`; unit 3 |
| FR-3.1 – FR-3.6 | **Not automated** — a gap. See §6.4 |
| FR-4.1 – FR-4.6 | Unit 4, 7; browser `updateDeliveryStatus`; FR-4.4, FR-4.5 by code inspection |
| FR-5.1 – FR-5.4 | Unit 4, 5; browser `updateDeliveryStatus` |
| FR-6.1 – FR-6.3 | Browser `createDelivery`, `updateDeliveryStatus` |
| FR-7.1 – FR-7.6 | Browser `searchAndFilterDeliveries` |
| FR-8.1 – FR-8.3 | Unit 6 |
| FR-9.1 – FR-9.8 | Jenkins builds #3 – #9 |

### 5.3 Defects found by testing

Testing found four issues that code review had not. They are recorded because
they justify the testing effort.

| # | Defect | Found by | Resolution |
|---|---|---|---|
| 1 | Search could not match on status | Manual use | Fixed; later reintroduced deliberately to prove the CI gate, then fixed again — build #7 |
| 2 | Selenium assertions ran against the page about to be replaced, causing intermittent failures | Local suite | Wait for the document to be discarded before asserting |
| 3 | Chrome's transient discarded-node error failed the build rather than retrying | Jenkins build #8 | Treat as "navigation committed" and poll; other errors still propagate |
| 4 | Fixed `sleep 8` was shorter than Tomcat's actual ~12s deploy | Jenkins | Poll the staging URL instead |

---

## 6. Known limitations

### 6.1 Source stock is not debited on dispatch

Crediting on arrival but not debiting on dispatch means the warehouse figure
overstates what is physically on site once shipments leave. This is a deliberate
simplification: dispatch is planned rather than confirmed, so debiting would
make the number wrong in a different way. Making this correct requires a product
decision, recorded as US-14.

### 6.2 Validation lives in the page

Input validation is in `index.jsp`, not in `FoodDistributionTracker`. This
weakens FR-4.6 as the only real service-layer guard, and it means the domain
service would accept a negative quantity if called directly. Moving validation
into the service is the correct fix and is on the backlog.

### 6.3 Output is not escaped

`search`, `message`, and `itemName` are written into the page without HTML
escaping, so a value containing markup is rendered as markup. Current input is
coordinator-entered and trusted, so this is not exploitable today, but it
becomes a stored XSS vector the moment untrusted input is introduced. Tracked
as US-16 and it should be closed before that happens.

### 6.4 Validation requirements are not automated

FR-3.1 to FR-3.6 are verified by reading the code, not by a test. The browser
suite deliberately does not exercise invalid input, because asserting on
rejection messages couples the tests to exact wording. This is a real coverage
gap.

### 6.5 No persistence

All state is in the HTTP session. A Tomcat restart loses every delivery, and the
application cannot run on more than one instance without sticky sessions.
Tracked as US-10 and it is the highest-value next item.

---

## 7. Traceability

| Document | Relationship |
|---|---|
| `docs/problem-statement.md` | Why the system exists; scope and success criteria |
| `docs/user-stories-and-backlog.md` | Requirements expressed as user stories, plus the backlog |
| `docs/selenium-test-plan.md` | How the browser requirements are tested |
| `docs/CI-REPORT.md` | Evidence for FR-9.x |
| `README.md` | How to build and run |
