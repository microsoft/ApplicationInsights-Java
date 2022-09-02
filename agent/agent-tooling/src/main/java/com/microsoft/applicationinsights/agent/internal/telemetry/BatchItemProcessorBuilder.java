// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static java.util.Objects.requireNonNull;

import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import java.util.concurrent.TimeUnit;

// copied from io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder
final class BatchItemProcessorBuilder {

  private static final long DEFAULT_SCHEDULE_DELAY_MILLIS =
      Long.getLong("applicationinsights.testing.batch-schedule-delay-millis", 5000);
  private static final int DEFAULT_EXPORT_TIMEOUT_MILLIS = 30_000;

  private static final int DEFAULT_MAX_QUEUE_SIZE = 2048;
  private static final int DEFAULT_MAX_EXPORT_BATCH_SIZE = 512;
  private static final int DEFAULT_MAX_PENDING_EXPORTS = 1;

  private final TelemetryItemExporter exporter;
  private final long scheduleDelayNanos =
      TimeUnit.MILLISECONDS.toNanos(DEFAULT_SCHEDULE_DELAY_MILLIS);
  private final long exporterTimeoutNanos =
      TimeUnit.MILLISECONDS.toNanos(DEFAULT_EXPORT_TIMEOUT_MILLIS);

  private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
  private int maxExportBatchSize = DEFAULT_MAX_EXPORT_BATCH_SIZE;
  private int maxPendingExports = DEFAULT_MAX_PENDING_EXPORTS;

  BatchItemProcessorBuilder(TelemetryItemExporter exporter) {
    this.exporter = requireNonNull(exporter, "exporter");
  }

  /**
   * Sets the maximum number of items that are kept in the queue before start dropping. More memory
   * than this value may be allocated to optimize queue access.
   *
   * <p>See the BatchItemProcessor class description for a high-level design description of this
   * class.
   *
   * <p>Default value is {@code 2048}.
   *
   * @param maxQueueSize the maximum number of items that are kept in the queue before start
   *     dropping.
   * @return this.
   * @see BatchItemProcessorBuilder#DEFAULT_MAX_QUEUE_SIZE
   */
  public BatchItemProcessorBuilder setMaxQueueSize(int maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
    return this;
  }

  /**
   * Sets the maximum batch size for every export. This must be smaller or equal to {@code
   * maxQueuedItems}.
   *
   * <p>Default value is {@code 512}.
   *
   * @param maxExportBatchSize the maximum batch size for every export.
   * @return this.
   * @see BatchItemProcessorBuilder#DEFAULT_MAX_EXPORT_BATCH_SIZE
   */
  public BatchItemProcessorBuilder setMaxExportBatchSize(int maxExportBatchSize) {
    checkArgument(maxExportBatchSize > 0, "maxExportBatchSize must be positive.");
    this.maxExportBatchSize = maxExportBatchSize;
    return this;
  }

  /**
   * The maximum number of exports that can be pending at any time.
   *
   * <p>The {@link BatchItemProcessor}'s single worker thread will keep processing as many batches
   * as it can without blocking on the {@link io.opentelemetry.sdk.common.CompletableResultCode}s
   * that are returned from the {@code spanExporter}, but it will limit the total number of pending
   * exports in flight to this number.
   *
   * <p>Default value is {@code 1}.
   *
   * @param maxPendingExports the maximum number of exports that can be pending at any time.
   * @return this.
   * @see BatchItemProcessorBuilder#DEFAULT_MAX_PENDING_EXPORTS
   */
  public BatchItemProcessorBuilder setMaxPendingExports(int maxPendingExports) {
    checkArgument(maxPendingExports > 0, "maxPendingExports must be positive.");
    this.maxPendingExports = maxPendingExports;
    return this;
  }

  /**
   * Returns a new {@link BatchItemProcessor} that batches, then converts items to proto and
   * forwards them to the given {@code exporter}.
   *
   * @return a new {@link BatchItemProcessor}.
   * @throws NullPointerException if the {@code exporter} is {@code null}.
   */
  public BatchItemProcessor build(String queueName) {
    return new BatchItemProcessor(
        exporter,
        scheduleDelayNanos,
        maxQueueSize,
        maxExportBatchSize,
        exporterTimeoutNanos,
        maxPendingExports,
        queueName);
  }
}
