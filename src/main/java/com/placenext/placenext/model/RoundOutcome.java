package com.placenext.placenext.model;

public enum RoundOutcome {
    PASSED("Passed"),
    FAILED("Failed");

    private final String label;

    RoundOutcome(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}