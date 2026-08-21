package com.vit.tracker;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AppTest {

    @Test
    public void testAddStock() {
        App app = new App();
        int result = app.addStock(20, 3);
        System.out.println("Checking App.addStock(20, 3), expected wrong value for CI failure demo.");
        assertEquals(24, result);
    }
}
