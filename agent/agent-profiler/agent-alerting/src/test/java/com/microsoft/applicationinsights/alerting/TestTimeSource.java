// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import java.time.Instant;

public class TestTimeSource extends TimeSource {
  Instant now = Instant.ofEpochMilli(0);

  @Override
  public Instant getNow() {
    return now;
  }

  public void increment(int milliseconds) {
    this.now = now.plusMillis(milliseconds);
  }
}
