package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.concurrent.atomic.AtomicLong;

public class NonessentialStatsbeat extends BaseStatsbeat {

  // Track local storage IO success and failure
  private static final String READ_FAILURE_COUNT = "Read Failure Count";
  private static final String WRITE_FAILURE_COUNT = "Write Failure Count";
  private final AtomicLong readFailureCount = new AtomicLong();
  private final AtomicLong writeFailureCount = new AtomicLong();

  protected NonessentialStatsbeat(CustomDimensions customDimensions) {
    super(customDimensions);
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    if (this.readFailureCount.get() != 0) {
      TelemetryItem telemetryItem =
          createStatsbeatTelemetry(
              telemetryClient, READ_FAILURE_COUNT, getReadFailureCount());
      telemetryClient.trackStatsbeatAsync(telemetryItem);
    }

    if (this.writeFailureCount.get() != 0) {
      TelemetryItem telemetryItem =
          createStatsbeatTelemetry(
              telemetryClient, WRITE_FAILURE_COUNT, getWriteFailureCount());
      telemetryClient.trackStatsbeatAsync(telemetryItem);
    }

    readFailureCount.set(0L);
    writeFailureCount.set(0L);
  }

  void incrementReadFailureCount() {
    readFailureCount.incrementAndGet();
  }

  long getReadFailureCount() {
    return readFailureCount.get();
  }

  void incrementWriteFailureCount() {
    writeFailureCount.incrementAndGet();
  }

  long getWriteFailureCount() {
    return writeFailureCount.get();
  }
}
