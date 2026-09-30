<%@ page import="com.vit.tracker.*,java.util.Map,java.util.TreeMap" %>

<%
    FoodDistributionTracker tracker =
        (FoodDistributionTracker) session.getAttribute("tracker");

    if (tracker == null) {
        tracker = new FoodDistributionTracker();
        session.setAttribute("tracker", tracker);

        tracker.addStock("C1", "Rice (kg)", 500);
    }

    String message = "";

    String search = request.getParameter("search");
    if (search == null) {
        search = "";
    }

    if ("POST".equalsIgnoreCase(request.getMethod())) {

        String action = request.getParameter("action");

        if ("advance".equals(action)) {

            String deliveryId = request.getParameter("deliveryId");
            Delivery target = tracker.getDelivery(deliveryId);

            if (target == null) {
                message = "Delivery not found: " + deliveryId;
            } else {
                DeliveryStatus[] all = DeliveryStatus.values();
                int nextIndex = target.getStatus().ordinal() + 1;

                if (nextIndex < all.length) {
                    tracker.updateDeliveryStatus(deliveryId, all[nextIndex]);
                    message = "Delivery " + deliveryId + " updated to " + all[nextIndex] + ".";
                } else {
                    message = "Delivery " + deliveryId + " is already " + all[nextIndex - 1] + ".";
                }
            }

        } else {

            String itemName = request.getParameter("itemName");
            String quantityText = request.getParameter("quantity");

            if (itemName == null || itemName.trim().isEmpty()) {
                message = "Item name is required.";
            } else {
                itemName = itemName.trim();

                try {
                    int quantity = Integer.parseInt(quantityText);

                    if (quantity <= 0) {
                        message = "Quantity must be greater than 0.";
                    } else {

                        int available = tracker.getStockLevel("C1", itemName);

                        if (quantity > available) {
                            message = "Insufficient stock: only " + available +
                                      " of " + itemName +
                                      " available at Central Warehouse (C1).";
                        } else {

                            String deliveryId = tracker.createDelivery(
                                "C1",
                                "C2",
                                itemName,
                                quantity
                            );

                            message = "Delivery created successfully. Delivery ID: " + deliveryId;

                        }

                    }

                } catch (Exception e) {
                    message = "Invalid quantity. Please enter a valid number.";
                }

            }

        }

    }

    int totalDeliveries = 0;
    int pendingDeliveries = 0;
    int inTransitDeliveries = 0;
    int deliveredDeliveries = 0;

    for (Delivery d : tracker.getDeliveriesForCenter("C1")) {
        totalDeliveries++;

        if (d.getStatus() == DeliveryStatus.PENDING) {
            pendingDeliveries++;
        } else if (d.getStatus() == DeliveryStatus.IN_TRANSIT) {
            inTransitDeliveries++;
        } else if (d.getStatus() == DeliveryStatus.DELIVERED) {
            deliveredDeliveries++;
        }
    }
%>

<!DOCTYPE html>
<html>
<head>
    <title>Food Distribution Tracker</title>

    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 40px;
        }

        table {
            border-collapse: collapse;
            margin-bottom: 30px;
        }

        th, td {
            border: 1px solid #999;
            padding: 8px 14px;
            text-align: left;
        }

        th {
            background: #f0f0f0;
        }

        input, button {
            padding: 8px;
            margin: 5px;
        }

        .message {
            margin: 15px 0;
            font-weight: bold;
        }
    </style>
</head>

<body>

    <h1>Food Distribution Tracker</h1>

    <h2>Create Delivery</h2>

    <form method="post">

        <label>Item Name:</label>

        <input
            type="text"
            name="itemName"
            value="Rice (kg)"
            required
        >

        <label>Quantity:</label>

        <input
            type="number"
            name="quantity"
            min="1"
            required
        >

        <button type="submit">
            Create Delivery
        </button>

    </form>

    <div class="message">
        <%= message %>
    </div>


    <h2>Delivery Summary</h2>

    <table>
        <tr>
            <th>Total Deliveries</th>
            <th>Pending</th>
            <th>In Transit</th>
            <th>Delivered</th>
        </tr>
        <tr>
            <td><%= totalDeliveries %></td>
            <td><%= pendingDeliveries %></td>
            <td><%= inTransitDeliveries %></td>
            <td><%= deliveredDeliveries %></td>
        </tr>
    </table>

    <h2>Live Inventory</h2>

    <table>

        <tr>
            <th>Center</th>
            <th>Item</th>
            <th>Stock</th>
        </tr>

        <%
            DistributionCenter[] centers = {
                new DistributionCenter("C1", "Central Warehouse"),
                new DistributionCenter("C2", "Downtown Center")
            };

            for (DistributionCenter c : centers) {

                Map<String, Integer> stock = new TreeMap<>(tracker.getStockForCenter(c.getId()));

                if (stock.isEmpty()) {
        %>
                    <tr>
                        <td><%= c %></td>
                        <td><i>no items</i></td>
                        <td>0</td>
                    </tr>
        <%
                    continue;
                }

                for (Map.Entry<String, Integer> entry : stock.entrySet()) {
        %>
                    <tr>
                        <td><%= c %></td>
                        <td><%= entry.getKey() %></td>
                        <td><%= entry.getValue() %></td>
                    </tr>
        <%
                }

            }
        %>

    </table>


    <h2>Delivery Status</h2>

    <form method="get">
        <label>Search Deliveries:</label>
        <input type="text" name="search" value="<%= search %>" placeholder="ID, item or status">
        <button type="submit">Search</button>
        <a href="index.jsp">Clear</a>
    </form>

    <br>

    <table>

        <tr>
            <th>ID</th>
            <th>From</th>
            <th>To</th>
            <th>Item</th>
            <th>Qty</th>
            <th>Status</th>
            <th>Action</th>
        </tr>

        <%
            for (Delivery d : tracker.getDeliveriesForCenter("C1")) {

                String searchText =
                    (d.getId() + " " +
                     d.getItemName()).toLowerCase();

                if (!searchText.contains(search.toLowerCase())) {
                    continue;
                }
        %>

        <tr>
            <td><%= d.getId() %></td>
            <td><%= d.getSourceCenterId() %></td>
            <td><%= d.getDestinationCenterId() %></td>
            <td><%= d.getItemName() %></td>
            <td><%= d.getQuantity() %></td>
            <td><%= d.getStatus() %></td>
            <td>
                <%
                    DeliveryStatus[] flow = DeliveryStatus.values();
                    int next = d.getStatus().ordinal() + 1;

                    if (next < flow.length) {
                %>
                        <form method="post" style="display: inline;">
                            <input type="hidden" name="action" value="advance">
                            <input type="hidden" name="deliveryId" value="<%= d.getId() %>">
                            <button type="submit">Mark <%= flow[next] %></button>
                        </form>
                <%
                    } else {
                %>
                        <small>Completed</small>
                <%
                    }
                %>
            </td>
        </tr>

        <%
            }
        %>

    </table>


    <p>
        <small>
            Build Tool: Maven | CI/CD: Jenkins | Server: Apache Tomcat
        </small>
    </p>

</body>
</html>
