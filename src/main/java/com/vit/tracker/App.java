package com.vit.tracker;

public class App {

    public static void main(String[] args) {
        FoodDistributionTracker tracker = new FoodDistributionTracker();

        DistributionCenter warehouse = new DistributionCenter("C1", "Central Warehouse");
        DistributionCenter centerA = new DistributionCenter("C2", "Downtown Center");

        tracker.addStock(warehouse.getId(), "Rice (kg)", 500);
        System.out.println("Stock at " + warehouse + ": " +
                tracker.getStockLevel(warehouse.getId(), "Rice (kg)") + " kg Rice");

        String deliveryId = tracker.createDelivery(warehouse.getId(), centerA.getId(), "Rice (kg)", 100);
        System.out.println("Created delivery: " + tracker.getDelivery(deliveryId));

        tracker.updateDeliveryStatus(deliveryId, DeliveryStatus.IN_TRANSIT);
        System.out.println("Updated: " + tracker.getDelivery(deliveryId));

        tracker.updateDeliveryStatus(deliveryId, DeliveryStatus.DELIVERED);
        System.out.println("Final: " + tracker.getDelivery(deliveryId));

        System.out.println("Stock at " + centerA + ": " +
                tracker.getStockLevel(centerA.getId(), "Rice (kg)") + " kg Rice");
    }
}
