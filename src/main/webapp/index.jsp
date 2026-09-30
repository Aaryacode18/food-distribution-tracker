<%@ page import="com.vit.tracker.*" %>
<%
    FoodDistributionTracker tracker = new FoodDistributionTracker();
    DistributionCenter warehouse = new DistributionCenter("C1", "Central Warehouse");
    DistributionCenter centerA = new DistributionCenter("C2", "Downtown Center");

    tracker.addStock(warehouse.getId(), "Rice (kg)", 500);
    String deliveryId = tracker.createDelivery(warehouse.getId(), centerA.getId(), "Rice (kg)", 100);
    tracker.updateDeliveryStatus(deliveryId, DeliveryStatus.IN_TRANSIT);
    tracker.updateDeliveryStatus(deliveryId, DeliveryStatus.DELIVERED);

    Delivery delivery = tracker.getDelivery(deliveryId);
%>
<!DOCTYPE html>
<html>
<head>
    <title>Food Distribution Tracker</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        table { border-collapse: collapse; margin-bottom: 30px; }
        th, td { border: 1px solid #999; padding: 8px 14px; text-align: left; }
        th { background: #f0f0f0; }
    </style>
</head>
<body>
    <h1>Food Distribution Tracker</h1>

    <h2>Live Inventory</h2>
    <table>
        <tr><th>Center</th><th>Item</th><th>Stock</th></tr>
        <tr>
            <td><%= warehouse %></td>
            <td>Rice (kg)</td>
            <td><%= tracker.getStockLevel(warehouse.getId(), "Rice (kg)") %></td>
        </tr>
        <tr>
            <td><%= centerA %></td>
            <td>Rice (kg)</td>
            <td><%= tracker.getStockLevel(centerA.getId(), "Rice (kg)") %></td>
        </tr>
    </table>

    <h2>Delivery Status</h2>
    <table>
        <tr><th>ID</th><th>From</th><th>To</th><th>Item</th><th>Qty</th><th>Status</th></tr>
        <tr>
            <td><%= delivery.getId() %></td>
            <td><%= delivery.getSourceCenterId() %></td>
            <td><%= delivery.getDestinationCenterId() %></td>
            <td><%= delivery.getItemName() %></td>
            <td><%= delivery.getQuantity() %></td>
            <td><%= delivery.getStatus() %></td>
        </tr>
    </table>

    <p><small>Build Tool: Maven | CI/CD: Jenkins | Server: Apache Tomcat</small></p>
</body>
</html>
