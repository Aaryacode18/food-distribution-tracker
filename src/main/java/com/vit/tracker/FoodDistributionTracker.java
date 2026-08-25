package com.vit.tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodDistributionTracker {

    private final Map<String, Map<String, Integer>> centerStock = new HashMap<>();
    private final Map<String, Delivery> deliveries = new HashMap<>();
    private int deliveryCounter = 0;

    public void addStock(String centerId, String itemName, int quantity) {
        centerStock.putIfAbsent(centerId, new HashMap<>());
        Map<String, Integer> stock = centerStock.get(centerId);
        stock.put(itemName, stock.getOrDefault(itemName, 0) + quantity);
    }

    public int getStockLevel(String centerId, String itemName) {
        return centerStock.getOrDefault(centerId, new HashMap<>()).getOrDefault(itemName, 0);
    }

    public Map<String, Integer> getStockForCenter(String centerId) {
        return centerStock.getOrDefault(centerId, new HashMap<>());
    }

    public String createDelivery(String sourceCenterId, String destinationCenterId,
                                  String itemName, int quantity) {
        deliveryCounter++;
        String deliveryId = "D" + deliveryCounter;
        Delivery delivery = new Delivery(deliveryId, sourceCenterId, destinationCenterId,
                                          itemName, quantity);
        deliveries.put(deliveryId, delivery);
        return deliveryId;
    }

    public void updateDeliveryStatus(String deliveryId, DeliveryStatus status) {
        Delivery delivery = deliveries.get(deliveryId);
        if (delivery == null) {
            throw new IllegalArgumentException("No delivery found with id: " + deliveryId);
        }
        delivery.setStatus(status);

        if (status == DeliveryStatus.DELIVERED) {
            addStock(delivery.getDestinationCenterId(), delivery.getItemName(), delivery.getQuantity());
        }
    }

    public Delivery getDelivery(String deliveryId) {
        return deliveries.get(deliveryId);
    }

    public List<Delivery> getDeliveriesForCenter(String centerId) {
        List<Delivery> result = new ArrayList<>();
        for (Delivery d : deliveries.values()) {
            if (d.getSourceCenterId().equals(centerId) || d.getDestinationCenterId().equals(centerId)) {
                result.add(d);
            }
        }
        return result;
    }
}
