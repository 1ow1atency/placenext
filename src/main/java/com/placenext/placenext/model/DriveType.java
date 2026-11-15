package com.placenext.placenext.model;

public enum DriveType {
    FULL_TIME("Full-time"),
    INTERNSHIP("Internship");

    private final String label;

    DriveType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}