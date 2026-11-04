// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

final class ExecutedMonikerTracker {

  static final Duration DEFAULT_RETENTION = Duration.ofMinutes(10);
  static final int DEFAULT_CAPACITY = 1024;

  private final Duration retention;
  private final int capacity;
  private final LinkedHashMap<String, Instant> executed = new LinkedHashMap<>();

  ExecutedMonikerTracker() {
    this(DEFAULT_RETENTION, DEFAULT_CAPACITY);
  }

  ExecutedMonikerTracker(Duration retention, int capacity) {
    if (retention.isNegative() || retention.isZero()) {
      throw new IllegalArgumentException("retention must be positive");
    }
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    this.retention = retention;
    this.capacity = capacity;
  }

  synchronized boolean tryMarkExecuted(String moniker, Instant now) {
    if (moniker == null || moniker.trim().isEmpty()) {
      return false;
    }

    removeExpired(now);
    String normalizedMoniker = moniker.trim();
    if (executed.containsKey(normalizedMoniker)) {
      return false;
    }

    while (executed.size() >= capacity) {
      Iterator<String> iterator = executed.keySet().iterator();
      iterator.next();
      iterator.remove();
    }
    executed.put(normalizedMoniker, now);
    return true;
  }

  private void removeExpired(Instant now) {
    Instant cutoff = now.minus(retention);
    Iterator<Map.Entry<String, Instant>> iterator = executed.entrySet().iterator();
    while (iterator.hasNext()) {
      if (iterator.next().getValue().isBefore(cutoff)) {
        iterator.remove();
      }
    }
  }
}
