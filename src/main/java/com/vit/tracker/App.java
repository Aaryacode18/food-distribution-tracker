package com.vit.tracker;

public class App {

    public int addStock(int currentStock, int incomingUnits) {
        return currentStock + incomingUnits;
    }

    public static void main(String[] args) {
        App app = new App();
        int updatedStock = app.addStock(20, 3);
        System.out.println("Food Distribution Tracker CI demo is running.");
        System.out.println("20 + 3 = " + updatedStock);
    }
}
