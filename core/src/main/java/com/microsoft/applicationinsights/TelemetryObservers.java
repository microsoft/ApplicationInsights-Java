package com.microsoft.applicationinsights;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;

public enum TelemetryObservers {
    INSTANCE;

    private final List<Consumer<TelemetryItem>> observers = new CopyOnWriteArrayList<>();


    public void addObserver(Consumer<TelemetryItem> observer) {
        observers.add(observer);
    }

    public List<Consumer<TelemetryItem>> getObservers() {
        return observers;
    }
}
