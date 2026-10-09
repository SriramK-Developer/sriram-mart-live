package com.srirammart.model;

public enum OrderStatus {
    PLACED("Order placed"), CONFIRMED("Confirmed"), SHIPPED("Shipped"),
    OUT_FOR_DELIVERY("Out for delivery"), DELIVERED("Delivered"), CANCELLED("Cancelled");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
    public boolean isFinal() { return this == DELIVERED || this == CANCELLED; }
    public boolean isCancellable() { return this == PLACED || this == CONFIRMED; }
    public String getCss() { return name().toLowerCase().replace('_', '-'); }
}
