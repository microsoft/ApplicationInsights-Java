/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.StatsbeatTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.localstorage.LocalStorageStats;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.concurrent.atomic.AtomicLong;

public class NonessentialStatsbeat extends BaseStatsbeat implements LocalStorageStats {

  // TODO when there are more non-essential statsbeat(s) in the future, try to separate different
  // kinds of non-essential statsbeat into different classes.

  // Track local storage IO success and failure
  private static final String READ_FAILURE_COUNT = "Read Failure Count";
  private static final String WRITE_FAILURE_COUNT = "Write Failure Count";
  private final AtomicLong readFailureCount = new AtomicLong();
  private final AtomicLong writeFailureCount = new AtomicLong();

  // only used by tests
  public NonessentialStatsbeat() {
    super(new CustomDimensions());
  }

  protected NonessentialStatsbeat(CustomDimensions customDimensions) {
    super(customDimensions);
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    long readFailures = readFailureCount.getAndSet(0);
    if (readFailures != 0) {
      StatsbeatTelemetryBuilder telemetryItem =
          createStatsbeatTelemetry(
              telemetryClient, READ_FAILURE_COUNT, (double) readFailureCount.get());
      telemetryClient.trackStatsbeatAsync(telemetryItem.build());
    }

    long writeFailures = readFailureCount.getAndSet(0);
    if (writeFailures != 0) {
      StatsbeatTelemetryBuilder telemetryItem =
          createStatsbeatTelemetry(
              telemetryClient, WRITE_FAILURE_COUNT, (double) writeFailureCount.get());
      telemetryClient.trackStatsbeatAsync(telemetryItem.build());
    }
  }

  @Override
  public void incrementReadFailureCount() {
    readFailureCount.incrementAndGet();
  }

  // used by tests only
  long getReadFailureCount() {
    return readFailureCount.get();
  }

  @Override
  public void incrementWriteFailureCount() {
    writeFailureCount.incrementAndGet();
  }

  // used by tests only
  public long getWriteFailureCount() {
    return writeFailureCount.get();
  }
}
