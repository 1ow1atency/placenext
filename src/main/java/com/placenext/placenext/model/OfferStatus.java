package com.placenext.placenext.model;

public enum OfferStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    DECLINED("Declined");

    private final String label;

    OfferStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}