package com.placenext.placenext.model;

public enum Branch {
    CSE("Computer Science & Engg"),
    IT("Information Technology"),
    ECE("Electronics & Communication"),
    EE("Electrical Engg"),
    ME("Mechanical Engg"),
    CE("Civil Engg"),
    CHE("Chemical Engg"),
    BT("Biotechnology");

    private final String label;

    Branch(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
