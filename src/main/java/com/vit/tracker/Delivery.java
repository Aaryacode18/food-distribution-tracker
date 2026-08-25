package com.vit.tracker;

public class Delivery {
    private final String id;
    private final String sourceCenterId;
    private final String destinationCenterId;
    private final String itemName;
    private final int quantity;
    private DeliveryStatus status;

    public Delivery(String id, String sourceCenterId, String destinationCenterId,
                     String itemName, int quantity) {
        this.id = id;
        this.sourceCenterId = sourceCenterId;
        this.destinationCenterId = destinationCenterId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.status = DeliveryStatus.PENDING;
    }

    public String getId() { return id; }
    public String getSourceCenterId() { return sourceCenterId; }
    public String getDestinationCenterId() { return destinationCenterId; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Delivery{" + id + ": " + quantity + "x " + itemName +
               " [" + sourceCenterId + " -> " + destinationCenterId + "] " + status + "}";
    }
}
