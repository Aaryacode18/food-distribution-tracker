package com.vit.tracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class FoodDistributionTrackerTest {

    @Test
    public void testAddAndGetStock() {
        FoodDistributionTracker tracker = new FoodDistributionTracker();
        tracker.addStock("C1", "Rice", 100);
        assertEquals(100, tracker.getStockLevel("C1", "Rice"));
    }

    @Test
    public void testAddStockAccumulates() {
        FoodDistributionTracker tracker = new FoodDistributionTracker();
        tracker.addStock("C1", "Rice", 100);
        tracker.addStock("C1", "Rice", 50);
        assertEquals(150, tracker.getStockLevel("C1", "Rice"));
    }

    @Test
    public void testCreateDeliveryStartsAsPending() {
        FoodDistributionTracker tracker = new FoodDistributionTracker();
        String id = tracker.createDelivery("C1", "C2", "Rice", 20);
        assertEquals(DeliveryStatus.PENDING, tracker.getDelivery(id).getStatus());
    }

    @Test
    public void testUpdateDeliveryStatus() {
        FoodDistributionTracker tracker = new FoodDistributionTracker();
        String id = tracker.createDelivery("C1", "C2", "Rice", 20);
        tracker.updateDeliveryStatus(id, DeliveryStatus.IN_TRANSIT);
        assertEquals(DeliveryStatus.IN_TRANSIT, tracker.getDelivery(id).getStatus());
    }

    @Test
    public void testDeliveredUpdatesDestinationStock() {
        FoodDistributionTracker tracker = new FoodDistributionTracker();
        tracker.addStock("C1", "Rice", 200);
        String id = tracker.createDelivery("C1", "C2", "Rice", 50);
        tracker.updateDeliveryStatus(id, DeliveryStatus.DELIVERED);
        assertEquals(50, tracker.getStockLevel("C2", "Rice"));
    }

    @Test
    public void testGetDeliveriesForCenter() {
        FoodDistributionTracker tracker = new FoodDistributionTracker();
        String id1 = tracker.createDelivery("C1", "C2", "Rice", 20);
        String id2 = tracker.createDelivery("C2", "C3", "Wheat", 15);
        assertEquals(1, tracker.getDeliveriesForCenter("C1").size());
        assertEquals(2, tracker.getDeliveriesForCenter("C2").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateNonexistentDeliveryThrows() {
        FoodDistributionTracker tracker = new FoodDistributionTracker();
        tracker.updateDeliveryStatus("D999", DeliveryStatus.IN_TRANSIT);
    }
}
