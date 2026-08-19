package com.vit.tracker;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AppTest {

    @Test
    public void testAddStock() {
        App app = new App();
        int result = app.addStock(20, 3);
        System.out.println("TEST PASSED: App.addStock(20, 3) returned " + result + ".");
        assertEquals(23, result);
    }
}
