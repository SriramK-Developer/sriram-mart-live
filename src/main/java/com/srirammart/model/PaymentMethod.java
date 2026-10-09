package com.srirammart.model;

public enum PaymentMethod {
    COD("Cash on Delivery"), UPI("UPI"), CARD("Credit / Debit Card"), NETBANKING("Net Banking");
    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
