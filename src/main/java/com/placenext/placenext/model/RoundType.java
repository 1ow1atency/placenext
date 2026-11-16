package com.placenext.placenext.model;

public enum RoundType {
    APTITUDE("Aptitude test"),
    CODING("Coding test"),
    GROUP_DISCUSSION("Group discussion"),
    TECHNICAL("Technical interview"),
    HR("HR interview");

    private final String label;

    RoundType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}