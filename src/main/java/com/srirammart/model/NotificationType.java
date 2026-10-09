package com.srirammart.model;

public enum NotificationType {
    ORDER("Orders"), OFFER("Offers"), ACCOUNT("Account"), SYSTEM("System");
    private final String label;
    NotificationType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
