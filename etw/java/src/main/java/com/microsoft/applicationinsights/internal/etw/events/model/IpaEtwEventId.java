package com.microsoft.applicationinsights.internal.etw.events.model;


public enum IpaEtwEventId {
    CRITICAL(1),
    ERROR(2),
    WARN(3),
    INFO(4);

    private final int idValue;

    private IpaEtwEventId(int idValue) {
        this.idValue = idValue;
    }

    public int value() {
        return idValue;
    }
}