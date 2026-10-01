# Demo Script

A click-by-click walkthrough for demonstrating this project. Follow it in order
— it is built to build a story rather than to tour screens.

**Before you start:** run `./start.sh`, then open the app URL it prints. Open a
second tab to the Jenkins job before you begin, because you will need it at step 7.

| What | Where |
|---|---|
| Application | `http://localhost:8081/food-distribution-tracker/` |
| Jenkins job | `http://localhost:8080/job/Food-Tracker-Pipeline/` |
| This report | `docs/CI-REPORT.md` |
| SRS | `docs/srs.md` |

---

## Step 1 — Open on the empty state (30s)

The app opens showing **Delivery Summary: 0 / 0 / 0 / 0** and a **Live
Inventory** at the warehouse holding five staples: `Oil (L) 150`,
`Rice (kg) 500`, `Salt (kg) 100`, `Sugar (kg) 200`, `Wheat (kg) 300`.

> "A warehouse stocked with five staples. Everything I do now is tracked from
> this one screen."

Point out the sections: **Add Stock**, **Create Delivery**, Delivery Summary,
Live Inventory, Delivery Status. Note there is no navigation — one screen.

---

## Step 2 — Create a delivery (45s)

Enter item `Rice (kg)`, quantity `40`, click **Create Delivery**.

The page reloads with a green message:

> Delivery created successfully. Delivery ID: D1

And the counts change to **1 / 1 / 0 / 0**. A row `D1 · C1 → C2 · Rice (kg) ·
40 · PENDING` appears with a **Mark IN_TRANSIT** button.

> "It gave the delivery an id. That's the thing I'll come back to."

---

## Step 3 — Add a brand new item (45s)

Use the **Add Stock** form: Center `Central Warehouse (C1)`, Item Name
`Lentils (kg)`, Quantity `80`, click **Add Stock**.

> Added 80 of Lentils (kg) at C1. New stock: 80.

`Lentils (kg)` now appears in Live Inventory, and the summary is still
`0 / 0 / 0 / 0` — adding stock is not a delivery.

> "That's what makes the app usable for anything the warehouse happens to
> handle. Before this, only the seeded item could be shipped and anything else
> failed with 'insufficient stock: only 0'."

## Step 4 — Ship the new item (30s)

In **Create Delivery**, item `Lentils (kg)`, quantity `10`, click **Create
Delivery**. It is accepted, because it is now stocked.

> "Add stock, then ship it. Two steps, and the second one would have failed
> before."

Click **Mark IN_TRANSIT**, then **Mark DELIVERED**. `Lentils (kg) 10` now
appears under Downtown Center (C2).

> "The item I just invented travelled the whole lifecycle."

## Step 5 — Show input validation (30s)

Try quantity `9999` and click **Create Delivery**.

> Insufficient stock: only 500 of Rice (kg) available at Central Warehouse (C1).

Try clearing the item name:

> Item name is required.

Try quantity `abc`:

> Invalid quantity. Please enter a valid number.

> "Three different bad inputs, three readable messages, and the page never
> breaks. No stack traces."

---

## Step 6 — Move it to in-transit (30s)

On row **D1**, click **Mark IN_TRANSIT**.

> Delivery D1 updated to IN_TRANSIT.

Counts become **2 / 0 / 1 / 1** (D1 in transit; D2 already delivered in step 4).
The button changes to **Mark DELIVERED**.

**Point at Live Inventory** — C2 holds `Lentils (kg) 10` but **no Rice at all**.

> "This is the important bit. D1 is in transit, not delivered, and C2 still has
> zero Rice. Stock hasn't moved for this delivery yet."

---

## Step 7 — Deliver it and watch stock land (45s) — *the main event*

Click **Mark DELIVERED**.

> Delivery D1 updated to DELIVERED.

Counts become **2 / 0 / 0 / 2**. And **Live Inventory** now shows Downtown
Centre (C2) holding **Rice (kg) 40**.

> "One click. C2 went from zero Rice to forty without anyone typing a number.
> That's the loop between moving food and knowing you have it."

---

## Step 8 — Search (30s)

Create a third delivery: `Rice (kg)`, `15`. Leave it `PENDING`. Counts → **3 / 1 / 0 / 2**.

In the search box, type `PENDING` and click **Search**. Only D3 is listed.

> "Searching by status."

Clear, then search `Lentils`. Only D2.

> "Searching by item name — including the item I invented in step 3."

Search `D1`. Only D1.

> "Searching by id."

Search `nosuchitem`. Empty list, no error.

> "No matches is a valid answer, not a crash."

Click **Clear**.

---

## Step 9 — The part that matters: the build gate (90s)

Switch to the Jenkins tab. Open **Build History**.

Point at the run sequence and say:

> "I introduced a deliberate regression — I made search stop matching on
> status. The seven unit tests all passed. The browser suite caught it, the
> build went red, and the deploy stage was skipped. The live application never
> served the broken version."

Then show `docs/CI-REPORT.md` for the exact log excerpts, the skipped stages,
and the unchanged WAR checksum.

> "Then I fixed it, and the next build went green and promoted."

If asked *"how do you know it didn't reach production?"* — the report records the
live WAR's SHA-256 before and after: unchanged, and its mtime still pointed at
the previous build.

---

## Step 10 — Prove it's not manual (60s) — optional but strong

Make a trivial commit on `main` and wait about two minutes without touching
Jenkins.

> "I pushed one commit and did not press anything. The job polls the branch
> every two minutes."

The build appears and runs on its own.

---

## Likely questions

**"Why is the data not saved anywhere?"**
> "It's held in the HTTP session. A Tomcat restart loses it. That was a
> deliberate scope decision for the MVP and it's the top item in the backlog —
> US-10, persistence. It's documented as a limitation in the SRS rather than
> glossed over."

**"You mentioned an XSS issue — is it fixed?"**
> "No, it's recorded as US-16. It's not exploitable today because all input is
> coordinator-entered and trusted, but it becomes a stored XSS the moment
> untrusted input enters, so it must be closed before that happens. It's in the
> SRS §6.3."

**"Doesn't the source warehouse stock stay at 540? You only shipped 55."**
> "Yes. Source stock is credited on arrival but not debited on dispatch. That
> was intentional — dispatch is planned, not confirmed, so debiting would make
> the number wrong in a different way. Making it correct needs a product
> decision, so it's US-14 in the backlog and noted in SRS §6.1."

**"Your tests were flaky at one point, weren't they?"**
> "Yes, and that's in the report. They failed one run in three, in three
> different ways. All three had the same cause: the assertions were running
> against the page that was about to be replaced. Adding a retry would have
> hidden it. I fixed the wait instead."

**"How do you know the CI gate is real and not decorative?"**
> "Build #7 and #8 both failed, and in both the deploy stage was skipped with
> 'skipped due to earlier failure(s)'. I checked the deployed WAR's checksum
> before and after — byte-identical."

---

## If something is broken

| Symptom | Fix |
|---|---|
| App URL not loading | `./start.sh` |
| Jenkins tab 403 | Log in with your Jenkins account |
| Counts show 0 unexpectedly | Expected. State is per browser session. |
| Port 8081 busy | `brew services stop tomcat && brew services start tomcat` |
| Want a clean slate | Close all tabs for the app and reopen. New session, fresh data. |
