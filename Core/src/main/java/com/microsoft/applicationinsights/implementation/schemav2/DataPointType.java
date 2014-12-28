package com.microsoft.applicationinsights.implementation.schemav2;

/**
 * Enum DataPointType.
 */
public enum DataPointType
{
    Measurement(0),
    Aggregation(1);

    private final int id;

    public int getValue() {
        return id;
    }

    DataPointType(int id) {
        this.id = id;
    }
}
