package com.placenext.placenext.model;

public enum DriveStatus {
    PENDING("Pending approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CLOSED("Closed");

    private final String label;

    DriveStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}