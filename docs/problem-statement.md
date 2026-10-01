# Problem Statement — Food Distribution Tracker

**Project:** Food Distribution Tracker
**Module:** Coursework deliverable, Tasks 1–10
**Status:** Implemented and deployed; see `README.md` to run it

---

## 1. Context

A regional relief organisation moves food from a central warehouse to community
centres. Today that movement is recorded on paper or in disconnected spreadsheets.
There is no single place where a coordinator can answer basic questions:

- How much rice is at the warehouse right now?
- How much did the downtown centre receive last week?
- Which deliveries are still sitting in transit?
- Did a delivery actually arrive, or is it still in transit?

## 2. The problem

Movement of goods is tracked, but the **outcome** is not. Inventory is only
decremented manually and reconciled by hand, so it drifts from reality. When it
does, nobody notices until a centre runs out of food it was supposed to have.

Three specific failures follow from this:

1. **No delivery lifecycle.** A shipment is created and then forgotten. There is
   no way to distinguish *planned* from *moving* from *arrived*, so nobody can
   tell whether a gap in stock is a delay or a shortfall.
2. **No arrival confirmation.** Nothing links a delivery reaching its
   destination to the stock recorded at that destination, so an arrival is never
   confirmed and inventory must be corrected manually.
3. **No visibility.** A coordinator managing several centres has to open several
   files to answer a single question. There is no summary.

## 3. Proposed solution

A small web application that records a delivery and then tracks it to arrival,
and updates inventory automatically when a delivery is confirmed delivered.

The workflow it supports is deliberately small and matches what a coordinator
actually does:

```
Create delivery  ->  PENDING  ->  IN_TRANSIT  ->  DELIVERED
                                                  |
                                                  +-- destination stock credited
```

When a delivery is marked `DELIVERED`, the destination centre's stock for that
item increases by the delivered quantity. This is the single behaviour that
closes the loop between movement and inventory.

## 4. Scope

### In scope

- Record a delivery of a quantity of an item from the warehouse to a centre
- Give each delivery an identifier so it can be referred to later
- Advance a delivery through its lifecycle one step at a time
- Credit destination stock automatically on arrival
- Show current stock per centre and per item
- Show live counts of deliveries by status
- Search and filter the delivery list by id, item, or status
- Reject invalid input with a message rather than a stack trace
- Automated tests, including browser tests that run in CI before a build is
  allowed to deploy

### Out of scope

This is a teaching deliverable and the scope is intentionally narrow. The
following are **not** in this release and are listed as known limitations rather
than defects:

| Excluded | Reason |
|---|---|
| Source stock decrement on dispatch | Simplification. Dispatch is planned, not confirmed, so stock is only credited on arrival. Tracked in the backlog. |
| Database persistence | State is held in the HTTP session. Tracked in the backlog. |
| User accounts and roles | Single trusted user assumed. Tracked in the backlog. |
| Editing or cancelling a delivery | Needs an audit model first. Tracked in the backlog. |
| Choosing source and destination | Fixed warehouse-to-downtown route for now. Tracked in the backlog. |

## 5. Users

| User | Need |
|---|---|
| **Dispatcher** | Record a delivery as soon as it is planned. |
| **Logistics coordinator** | See what is in transit and confirm arrivals. |
| **Centre manager** | Know how much stock their centre holds. |

All three are treated as one trusted role in this release.

## 6. Success criteria

The application is considered successful if:

1. A dispatcher can create a delivery and see it listed in under 10 seconds.
2. A coordinator can confirm an arrival and see destination stock increase as a
   direct result of that one action.
3. A coordinator can answer "what is in transit" from one screen without
   opening another file.
4. Invalid input produces a readable message, not a crash.
5. Every change to `main` is built and tested automatically, and **a failing
   test prevents the new version from reaching the live application.**

Criterion 5 is the one that distinguishes this from a script that happens to
work. See `docs/CI-REPORT.md` for the evidence.

## 7. Risks

| Risk | Mitigation |
|---|---|
| State lost on restart | Documented limitation; persistence is the first backlog item. |
| Unescaped output enables stored XSS | Known issue, recorded in the backlog; all current values are coordinator-entered and trusted. |
| Browser tests become flaky and the gate is ignored | Flakes were root-caused and fixed rather than retried blindly; see `docs/selenium-test-plan.md`. |

## 8. Related documents

| Document | Covers |
|---|---|
| `docs/user-stories-and-backlog.md` | Tasks 2 and 6: user stories, backlog, sprint plan, updated backlog |
| `docs/srs.md` | Task 3: functional and non-functional requirements |
| `docs/selenium-test-plan.md` | Task 9: browser test plan |
| `docs/CI-REPORT.md` | Task 10: CI evidence, including a deliberate defect that was caught |
| `README.md` | How to build, run, and use the application |
| `DEMO.md` | Click-by-click walkthrough for demonstrating the application |
