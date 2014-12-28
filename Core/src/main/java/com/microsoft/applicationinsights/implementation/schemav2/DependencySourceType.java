package com.microsoft.applicationinsights.implementation.schemav2;

/**
 * Enum DependencySourceType.
 */
public enum DependencySourceType {
    Undefined(0),
    Aic(1),
    Apmc(2);

    private final int id;

    public int getValue() {
        return id;
    }

    DependencySourceType(int id) {
        this.id = id;
    }
}
