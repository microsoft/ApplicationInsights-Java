// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor.collectors;

class IncrementalCounter {

  private static final long NO_PREVIOUS = -1;

  private long lastSeenValue;
  private long increment;

  public IncrementalCounter() {
    reset();
  }

  public boolean hasPrevious() {
    return increment != NO_PREVIOUS;
  }

  public void newValue(long newValue) {
    if (lastSeenValue != NO_PREVIOUS) {
      if (newValue > 0) {
        increment = newValue - lastSeenValue;
      } else {
        increment = (Long.MAX_VALUE - lastSeenValue) + (newValue - Long.MIN_VALUE);
      }
      if (increment < 0) {
        increment = 0;
      }
    }
    lastSeenValue = newValue;
  }

  public long getIncrement() {
    return increment;
  }

  public long getValue() {
    return lastSeenValue;
  }

  @Override
  public String toString() {
    return "IncrementalCounter [lastSeenValue=" + lastSeenValue + ", increment=" + increment + "]";
  }

  public void reset() {
    lastSeenValue = NO_PREVIOUS;
    increment = NO_PREVIOUS;
  }
}
