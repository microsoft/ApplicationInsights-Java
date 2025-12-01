// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SuppressWarnings(
    "ImmutableEnumChecker") // mutable enum state is intentional and properly synchronized
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
