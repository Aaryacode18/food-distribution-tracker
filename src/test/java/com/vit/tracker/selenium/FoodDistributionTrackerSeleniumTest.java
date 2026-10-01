package com.vit.tracker.selenium;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Selenium WebDriver tests for the three critical user journeys of the
 * Food Distribution Tracker.
 *
 * <p>These tests drive a real Chrome browser against a running deployment, so
 * they only run when the application is already deployed. They are excluded
 * from the default {@code mvn test} and are run explicitly with:
 *
 * <pre>
 *   mvn test -Pselenium
 * </pre>
 *
 * <p>Override the target URL with {@code -Dapp.url=...}. Each test method gets a
 * fresh browser, and the application stores state in the HTTP session, so every
 * test starts from the seeded state (C1 stocked with five staples and no
 * deliveries).
 */
public class FoodDistributionTrackerSeleniumTest {

    private static final String APP_URL =
            System.getProperty("app.url", "http://localhost:8081/food-distribution-tracker/");

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private WebDriver driver;
    private WebDriverWait wait;

    /**
     * Captures a screenshot when a test fails, so the failure can be diagnosed
     * from the Jenkins build artefacts rather than only from the stack trace.
     */
    @Rule
    public final TestWatcher screenshotOnFailure = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            capture(description.getMethodName());
        }

        @Override
        protected void finished(Description description) {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
        }
    };

    private void capture(String testName) {
        if (!(driver instanceof TakesScreenshot)) {
            return;
        }
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dir = new File("target/selenium-screenshots");
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            File dest = new File(dir, testName + ".png");
            Files.copy(src.toPath(), dest.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[screenshot] failure captured: " + dest.getAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            System.out.println("[screenshot] could not capture for " + testName + ": " + ex);
        }
    }

    @Before
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1400,1000");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, TIMEOUT);
        driver.get(APP_URL);
    }

    // ---------------------------------------------------------------- journey 1

    /**
     * Journey 1: create a delivery.
     *
     * <p>Fill the create form, submit, and assert that the success message is
     * shown, that the delivery appears in the table, and that the summary
     * indicators move to 1 total / 1 pending.
     */
    @Test
    public void createDelivery() {
        assertEquals("Summary starts empty for a fresh session",
                "0,0,0,0", summary());

        createDelivery("Rice (kg)", "40");

        String message = message();
        assertTrue("Success message should be shown, was: " + message,
                message.contains("Delivery created successfully"));
        assertTrue("Success message should include a delivery id, was: " + message,
                message.contains("D1"));

        assertEquals("Exactly one delivery row should be listed", 1, rowCount());
        assertTrue("Table should show the item that was entered",
                tableText().contains("Rice (kg)"));
        assertTrue("Table should show the quantity that was entered",
                tableText().contains("40"));
        assertTrue("Table should show PENDING status",
                tableText().contains("PENDING"));

        assertEquals("Summary should now show 1 total / 1 pending / 0 in transit / 0 delivered",
                "1,1,0,0", summary());

        assertTrue("Central Warehouse should still hold 500 kg",
                inventoryContains("Central Warehouse (C1)", "500"));
    }

    // ---------------------------------------------------------------- journey 2

    /**
     * Journey 2: search and filter deliveries.
     *
     * <p>Create two deliveries, then confirm that searching by status narrows
     * the table, that searching by item name works, and that a term matching
     * nothing returns no rows.
     */
    @Test
    public void searchAndFilterDeliveries() {
        assertEquals("Each test must start from a clean session",
                "0,0,0,0", summary());

        createDelivery("Rice (kg)", "40");
        createDelivery("Rice (kg)", "15");
        advanceToInTransit();

        assertEquals("Both deliveries should be listed before searching",
                2, rowCount());

        search("rice");
        assertEquals("Searching by item name should match both deliveries",
                2, rowCount());
        assertTrue("Filtered rows should still show the item",
                tableText().contains("Rice (kg)"));

        search("in_transit");
        assertEquals("Searching by status should match only the advanced delivery",
                1, rowCount());
        assertTrue("Remaining row should be the IN_TRANSIT delivery",
                tableText().contains("IN_TRANSIT"));
        assertFalse("Pending delivery should be filtered out",
                tableText().contains("PENDING"));

        search("D1");
        assertEquals("Searching by delivery id should match exactly one row",
                1, rowCount());

        search("nosuchitem");
        assertEquals("A search with no matches should return no rows",
                0, rowCount());
    }

    // ---------------------------------------------------------------- journey 3

    /**
     * Journey 3: check and update delivery status.
     *
     * <p>Walk a delivery through PENDING -> IN_TRANSIT -> DELIVERED, asserting
     * the summary indicators at each step and confirming that the destination
     * center's inventory is credited only once the delivery is completed.
     */
    @Test
    public void updateDeliveryStatus() {
        createDelivery("Rice (kg)", "40");
        assertEquals("New delivery starts PENDING", "1,1,0,0", summary());

        advanceToInTransit();
        assertEquals("After advancing, the delivery is IN_TRANSIT",
                "1,0,1,0", summary());
        assertTrue("Table should show IN_TRANSIT", tableText().contains("IN_TRANSIT"));
        assertFalse("Destination inventory should not be credited while in transit",
                inventoryContains("Downtown Center (C2)", "40"));

        advanceToDelivered();
        assertEquals("After the second advance, the delivery is DELIVERED",
                "1,0,0,1", summary());
        assertTrue("Table should show DELIVERED", tableText().contains("DELIVERED"));
        assertTrue("Destination inventory should be credited on completion",
                inventoryContains("Downtown Center (C2)", "40"));
        assertTrue("Completed delivery should show no further action",
                tableText().contains("Completed"));
    }

    /**
     * Stock can be added for a centre, and an item that did not previously exist
     * becomes newly deliverable.
     *
     * <p>This is what makes the application usable for more than the seeded
     * item: without it, creating a delivery for anything the warehouse has not
     * been stocked with fails with an insufficient-stock message.
     */
    @Test
    public void addStockForNewItem() {
        assertTrue("A seeded item should start stocked",
                inventoryContains("Central Warehouse (C1)", "Rice (kg)"));
        assertFalse("An unstocked item should not be listed yet",
                inventoryText("Central Warehouse (C1)").contains("Lentils (kg)"));

        addStock("C1", "Lentils (kg)", "80");

        assertTrue("The new item should appear in the warehouse inventory",
                inventoryText("Central Warehouse (C1)").contains("Lentils (kg)"));
        assertEquals("The new item should show exactly the quantity added",
                "Central Warehouse (C1) Lentils (kg) 80",
                inventoryItemRow("Central Warehouse (C1)", "Lentils (kg)"));
        assertEquals("Adding stock should not create a delivery",
                "0,0,0,0", summary());

        // The point of adding stock is that the item can then be shipped.
        createDelivery("Lentils (kg)", "10");
        assertEquals("The newly stocked item should be deliverable",
                "1,1,0,0", summary());
        advanceToInTransit();
        advanceToDelivered();
        assertEquals("The new item should reach the destination with the delivered amount",
                "Downtown Center (C2) Lentils (kg) 10",
                inventoryItemRow("Downtown Center (C2)", "Lentils (kg)"));
    }

    // ------------------------------------------------------------------ helpers

    /** Message Chrome uses when the node it is asked about has been discarded. */
    private static final String NODE_REPLACED = "does not belong to the document";

    /**
     * Clicks a control and blocks until the resulting page navigation commits.
     *
     * <p>Every action in this application is a full form POST or GET, so the
     * document is replaced. Waiting on content alone is not sufficient: the
     * value typed into the search box is already present before the new
     * response arrives, and the previous status message may already contain
     * the text being waited for, so both conditions can be satisfied against
     * the page that is about to be replaced.
     *
     * <p>Instead an element that exists on every rendered page is captured
     * before the click and waited on until it goes stale, which only happens
     * once the old document has been discarded.
     */
    private void clickAndWaitForReload(WebElement toClick) {
        WebElement marker = driver.findElement(By.tagName("h1"));

        // Chrome's DevTools protocol can transiently reject a click with
        // "Node with given id does not belong to the document" when it is
        // issued while the previous document is still being torn down.
        // Re-locating the element and retrying makes the suite reliable
        // without hiding a genuine failure, which still propagates.
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                toClick.click();
                last = null;
                break;
            } catch (WebDriverException e) {
                if (!isNodeReplaced(e)) {
                    throw e;
                }
                last = e;
                sleep(500);
            }
        }
        if (last != null) {
            throw last;
        }

        // Wait for the old document to actually be discarded.
        //
        // Selenium's own stalenessOf() cannot be used here: it probes the node
        // with isElementEnabled, which surfaces the transient DevTools error
        // above as a WebDriverException, and wait.until propagates that rather
        // than polling again. That made the build fail whenever Chrome replied
        // at the wrong moment. The same condition is probed directly below so
        // it counts as "navigation happened" instead of as an error.
        wait.until(d -> {
            try {
                marker.isEnabled();
                return false;
            } catch (StaleElementReferenceException e) {
                return true;
            } catch (WebDriverException e) {
                if (isNodeReplaced(e)) {
                    return true;
                }
                throw e;
            }
        });
    }

    private static boolean isNodeReplaced(WebDriverException e) {
        String message = e.getMessage();
        return message != null && message.contains(NODE_REPLACED);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits until the message div contains the expected text.
     *
     * <p>Must only be called after {@link #clickAndWaitForReload(WebElement)}.
     */
    private void waitForMessage(String expected) {
        wait.until(d -> {
            try {
                String text = d.findElement(By.cssSelector("div.message")).getText().trim();
                return text.contains(expected);
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                // The page is still navigating; the poll will retry.
                return false;
            }
        });
    }

    /** Submits the create form and waits for the success message. */
    private void createDelivery(String item, String quantity) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("itemName")));
        driver.findElement(By.name("itemName")).clear();
        driver.findElement(By.name("itemName")).sendKeys(item);
        driver.findElement(By.name("quantity")).clear();
        driver.findElement(By.name("quantity")).sendKeys(quantity);
        clickAndWaitForReload(driver.findElement(By.cssSelector("form button[type=submit]")));
        waitForMessage("Delivery created successfully");
    }

    /**
     * Clicks the action button in the row belonging to a specific delivery.
     *
     * <p>Rows are rendered from a {@code HashMap}, so their order is not
     * guaranteed. Selecting the row by delivery id rather than by position
     * keeps the test deterministic regardless of ordering.
     */
    private void advanceDelivery(String deliveryId, String buttonText) {
        WebElement button = driver.findElement(By.xpath(
                "//tr[td[normalize-space()='" + deliveryId + "']]"
                        + "//button[normalize-space(text())='" + buttonText + "']"));
        clickAndWaitForReload(button);
        waitForMessage("updated to");
    }

    private void advanceToInTransit() {
        advanceDelivery("D1", "Mark IN_TRANSIT");
    }

    /** Submits the Add Stock form and waits for the success message. */
    private void addStock(String centerId, String item, String quantity) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("stockItemName")));
        new Select(driver.findElement(By.name("stockCenter"))).selectByValue(centerId);
        driver.findElement(By.name("stockItemName")).clear();
        driver.findElement(By.name("stockItemName")).sendKeys(item);
        driver.findElement(By.name("stockQuantity")).clear();
        driver.findElement(By.name("stockQuantity")).sendKeys(quantity);
        clickAndWaitForReload(
                driver.findElement(By.xpath("//button[normalize-space(text())='Add Stock']")));
        waitForMessage("New stock:");
    }

    private void advanceToDelivered() {
        advanceDelivery("D1", "Mark DELIVERED");
    }

    private void search(String term) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("search")));
        driver.findElement(By.name("search")).clear();
        driver.findElement(By.name("search")).sendKeys(term);
        clickAndWaitForReload(
                driver.findElement(By.xpath("//button[normalize-space(text())='Search']")));

        // The reloaded page echoes the term back into the search box, which
        // confirms the filtered response was rendered.
        wait.until(d -> {
            try {
                return term.equals(d.findElement(By.name("search")).getAttribute("value"));
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return false;
            }
        });
    }

    /** Returns the trimmed text of the message div. */
    private String message() {
        return driver.findElement(By.cssSelector("div.message")).getText().trim();
    }

    /** Returns the text of the whole deliveries table. */
    private String tableText() {
        return driver.findElement(By.xpath("//h2[text()='Delivery Status']/following::table[1]"))
                .getText();
    }

    /** Counts data rows in the deliveries table, excluding the header row. */
    private int rowCount() {
        List<WebElement> rows = driver.findElements(
                By.xpath("//h2[text()='Delivery Status']/following::table[1]//tr[position()>1]"));
        return rows.size();
    }

    /** Returns the four summary values joined: total, pending, in transit, delivered. */
    private String summary() {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//h2[text()='Delivery Summary']")));
        List<String> cells = new ArrayList<>();
        for (WebElement td : driver.findElements(
                By.xpath("//h2[text()='Delivery Summary']/following::table[1]//tr[2]/td"))) {
            cells.add(td.getText().trim());
        }
        assertEquals("Summary should expose exactly four indicators", 4, cells.size());
        return String.join(",", cells);
    }

    /**
     * Every inventory row belonging to a center, joined into one string.
     *
     * <p>A center has one row per item, so matching on a single row is only
     * valid while a center holds at most one item. Once stock for several items
     * is added, all of the center's rows are joined so an assertion about the
     * center does not silently depend on which row happens to come first.
     */
    private String inventoryText(String center) {
        StringBuilder joined = new StringBuilder();
        for (WebElement row : driver.findElements(By.xpath(
                "//h2[text()='Live Inventory']/following::table[1]"
                        + "//tr[td[normalize-space()='" + center + "']]"))) {
            joined.append(row.getText().replaceAll("\\s+", " ")).append(" ");
        }
        return joined.toString().trim();
    }

    /**
     * The single inventory row for a given center and item.
     *
     * <p>Prefer this over {@link #inventoryText(String)} when the assertion is
     * about one specific item, so a value such as {@code 80} cannot be
     * satisfied by a different item's stock level.
     */
    private String inventoryItemRow(String center, String item) {
        return driver.findElement(By.xpath(
                "//h2[text()='Live Inventory']/following::table[1]"
                        + "//tr[td[normalize-space()='" + center + "']"
                        + " and td[normalize-space()='" + item + "']]"))
                .getText().replaceAll("\\s+", " ").trim();
    }

    /** True when any of the center's inventory rows shows the given text. */
    private boolean inventoryContains(String center, String stock) {
        return inventoryText(center).contains(stock);
    }
}
