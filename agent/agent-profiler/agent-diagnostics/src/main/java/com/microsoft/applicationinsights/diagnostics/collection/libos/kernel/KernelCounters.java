// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.kernel;

import java.beans.ConstructorProperties;

public class KernelCounters {

  private final long contextSwitches;
  private final long userTime;
  private final long systemTime;
  private final long idleTime;
  private final long waitTime;

  private final long procsRunnable;
  private final long procsBlocked;

  @ConstructorProperties({
    "contextSwitches",
    "userTime",
    "systemTime",
    "idleTime",
    "waitTime",
    "procsRunnable",
    "procsBlocked"
  })
  public KernelCounters(
      long contextSwitches,
      long userTime,
      long systemTime,
      long idleTime,
      long waitTime,
      long procsRunnable,
      long procsBlocked) {
    this.contextSwitches = contextSwitches;
    this.userTime = userTime;
    this.systemTime = systemTime;
    this.idleTime = idleTime;
    this.waitTime = waitTime;
    this.procsRunnable = procsRunnable;
    this.procsBlocked = procsBlocked;
  }

  public long getContextSwitches() {
    return contextSwitches;
  }

  public long getUserTime() {
    return userTime;
  }

  public long getSystemTime() {
    return systemTime;
  }

  public long getIdleTime() {
    return idleTime;
  }

  public long getWaitTime() {
    return waitTime;
  }

  public long getProcsRunnable() {
    return procsRunnable;
  }

  public long getProcsBlocked() {
    return procsBlocked;
  }
}
