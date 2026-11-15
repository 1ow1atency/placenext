package com.placenext.placenext.model;

public enum ApplicationStatus {
    APPLIED("Applied"),
    SHORTLISTED("Shortlisted"),
    REJECTED("Rejected"),
    SELECTED("Selected"),
    WITHDRAWN("Withdrawn");

    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}