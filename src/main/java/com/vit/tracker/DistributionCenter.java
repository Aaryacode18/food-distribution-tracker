package com.vit.tracker;

public class DistributionCenter {
    private final String id;
    private final String name;

    public DistributionCenter(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
