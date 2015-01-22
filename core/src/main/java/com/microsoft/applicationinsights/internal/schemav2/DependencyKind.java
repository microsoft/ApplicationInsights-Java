package com.microsoft.applicationinsights.internal.schemav2;

/**
 * Enum DependencyKind.
 */
public enum DependencyKind
{
    Undefined(0),
    HttpOnly(1),
    HttpAny(2),
    SQL(3);

    private final int id;

    public int getValue() {
        return id;
    }

    DependencyKind(int id) {
        this.id = id;
    }
}
