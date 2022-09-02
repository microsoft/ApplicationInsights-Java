// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitortests;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Watchdog timer, unless reset before the timeout, will invoke the onComplete method. */
public class WatchDog {
  private final ScheduledExecutorService executor;
  private final Runnable onComplete;
  private final int time;
  private ScheduledFuture<?> future;

  public WatchDog(Runnable onComplete, int time) {
    this.onComplete = onComplete;
    this.time = time;
    executor = Executors.newSingleThreadScheduledExecutor();
  }

  public void start() {
    future = executor.schedule(onComplete, time, TimeUnit.MILLISECONDS);
  }

  public void reset() {
    future.cancel(true);
    start();
  }
}
