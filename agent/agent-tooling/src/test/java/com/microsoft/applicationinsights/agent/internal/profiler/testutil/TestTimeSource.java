// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.testutil;

import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import java.time.Instant;

public class TestTimeSource extends TimeSource {

  private Instant now = Instant.now();

  @Override
  public Instant getNow() {
    return now;
  }

  public void setNow(Instant now) {
    this.now = now;
  }

  public void increment(int milliseconds) {
    now = now.plusMillis(milliseconds);
  }
}
