# Food Distribution Tracker

A small Java web application for tracking food stock movements between distribution
centers. Deliveries are created, moved through a status flow, and summarised on a
live dashboard.

Built as part of a DevOps coursework project.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 (compiled with `maven.compiler.release=21`) |
| Build tool | Maven (primary). `build.gradle` and `build.xml` are also present, see [Known issues](#known-issues) |
| Web | JSP on Apache Tomcat (WAR deployment) |
| Persistence | In-memory (`HashMap` held in `HttpSession`) — no database |
| Tests | JUnit 4 |
| CI/CD | Jenkins pipeline |

## Prerequisites

- JDK 21 or newer (`java -version`)
- Maven 3.8+ (`mvn -version`)
- Apache Tomcat 9+ listening on port **8081**

## Build and run

```bash
# 1. Compile, run unit tests, and package the WAR
mvn clean package

# 2. Deploy the WAR to Tomcat
cp -f target/food-distribution-tracker.war \
      /opt/homebrew/opt/tomcat/libexec/webapps/food-distribution-tracker.war

# 3. Open the application
open http://localhost:8081/food-distribution-tracker/
```

Tomcat auto-explodes the WAR and recompiles `index.jsp` on change. To run the
console demo instead:

```bash
java -cp target/classes com.vit.tracker.App
```

## Features

- **Data entry** — create a delivery from `C1` (Central Warehouse) to `C2`
  (Downtown Center) for any item and quantity.
- **Searchable dashboard** — filter the delivery table by ID, item, or status.
- **Summary indicators** — Total / Pending / In Transit / Delivered counts.
- **Status drill-down** — each delivery row has a button that advances
  `PENDING → IN_TRANSIT → DELIVERED`.
- **Alert / exception view** — invalid quantity, non-positive quantity, blank
  item name, and insufficient stock at the source center are all rejected with
  a message.
- **Live inventory** — rendered from real stock per center via
  `getStockForCenter()`, with an empty-state row.

Marking a delivery `DELIVERED` credits the quantity to the destination center's
stock.

## Project structure

```
src/main/java/com/vit/tracker/
  App.java                     console demo entry point
  FoodDistributionTracker.java core service: stock + delivery logic
  Delivery.java                delivery entity
  DeliveryStatus.java          PENDING / IN_TRANSIT / DELIVERED enum
  DistributionCenter.java      center entity (id + name)
src/main/webapp/
  index.jsp                    the entire UI (markup, CSS, and scriptlets)
src/test/java/com/vit/tracker/
  FoodDistributionTrackerTest.java       unit tests
  selenium/
    FoodDistributionTrackerSeleniumTest.java  browser tests (`-Pselenium`)
docs/
  selenium-test-plan.md
```

The web layer is a single JSP. There is no servlet, `web.xml`, or DAO layer.

## Testing

```bash
# Unit tests. The Selenium suite is excluded from this run.
mvn test
```

7 JUnit tests cover stock accumulation, delivery creation, status transitions,
stock crediting on delivery, per-center filtering, and the unknown-delivery error.

Browser tests live in a separate profile so that a plain `mvn test` stays fast
and needs neither a browser nor a running Tomcat:

```bash
mvn test -Pselenium
```

3 Selenium tests cover the three critical journeys: creating a delivery,
searching and filtering deliveries, and updating status while watching the
destination inventory update. They run headless in Chrome against the deployed
application, and capture a screenshot on failure. See
[`docs/selenium-test-plan.md`](docs/selenium-test-plan.md) for the full plan.

## Branches

| Branch | Purpose |
|---|---|
| `main` | Release baseline. Tagged releases are cut from here. |
| `develop` | Integration branch. Feature branches merge here first. |
| `feature/*` | One feature per branch. |

Workflow: branch from `develop` → commit → push → pull request → review → merge
into `develop`. Releases are tagged on `main`.

## CI/CD

`Jenkinsfile` defines a declarative pipeline, and the job polls the `main` branch
every two minutes, so every pushed commit builds automatically. The job reads
`Jenkinsfile` from the repository, so pipeline changes are versioned with the code.

```
Checkout -> Compile -> Unit Test -> Package -> Deploy to Staging
         -> Browser Test -> Deploy -> Verify
```

The browser suite is the release gate. Because Selenium needs a running
application, the new WAR is first deployed to a throwaway staging context
(`food-distribution-tracker-staging`) and the suite runs there. The WAR is only
promoted to the live `food-distribution-tracker` context if every test passes, so
a failing browser test leaves the live application on the previous build. The
staging context is removed in a `cleanup` block on any outcome.

Each build publishes both JUnit result sets (7 unit + 3 browser) and archives
`target/selenium-screenshots/*.png`, which is only non-empty when a browser test
fails. A failed test therefore stops the build before anything is promoted.

Build parameters (`BRANCH`, `DEPLOY_ENV`, `TOMCAT_WEBAPPS`, `APP_CONTEXT`,
`STAGING_CONTEXT`, `APP_PORT`, `JAVA_HOME_PATH`, `RUN_SELENIUM`) are editable
under *Build with Parameters*. Note that Jenkins keeps the last-used value of an
existing parameter rather than the `defaultValue` in the `Jenkinsfile`, so a
parameter changed to `true` in the pipeline file still needs to be flipped in the
job UI once.

## Known issues

- `build.gradle` requests a Java 21 toolchain and fails on machines that only
  have JDK 22 or newer. Maven is the supported build.
- `build.xml` still references `com.vit.tracker.AppTest`, a class that no longer
  exists, so `ant build` fails.
- State lives in `HttpSession`, so all data is lost on a Tomcat restart or when
  the session expires.
- Stock is not deducted from the source center when a delivery is created, so
  warehouse totals never decrease.
- There is no "Add Stock" form, so only the seeded `Rice (kg)` item can exist.

## Author

Aarya Panchal
