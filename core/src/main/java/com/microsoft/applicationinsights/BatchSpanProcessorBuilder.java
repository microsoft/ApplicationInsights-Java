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

package com.microsoft.applicationinsights;

import static io.opentelemetry.api.internal.Utils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

// copied from io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder
final class BatchSpanProcessorBuilder {

  private static final long DEFAULT_SCHEDULE_DELAY_MILLIS = 5000;
  private static final int DEFAULT_MAX_QUEUE_SIZE = 2048;
  private static final int DEFAULT_MAX_EXPORT_BATCH_SIZE = 512;
  private static final int DEFAULT_EXPORT_TIMEOUT_MILLIS = 30_000;

  private final TelemetryChannel spanExporter;
  private long scheduleDelayNanos = TimeUnit.MILLISECONDS.toNanos(DEFAULT_SCHEDULE_DELAY_MILLIS);
  private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
  private int maxExportBatchSize = DEFAULT_MAX_EXPORT_BATCH_SIZE;
  private long exporterTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(DEFAULT_EXPORT_TIMEOUT_MILLIS);

  BatchSpanProcessorBuilder(TelemetryChannel spanExporter) {
    this.spanExporter = requireNonNull(spanExporter, "spanExporter");
  }

  // TODO: Consider to add support for constant Attributes and/or Resource.

  /**
   * Sets the delay interval between two consecutive exports. If unset, defaults to {@value
   * DEFAULT_SCHEDULE_DELAY_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setScheduleDelay(long delay, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(delay >= 0, "delay must be non-negative");
    scheduleDelayNanos = unit.toNanos(delay);
    return this;
  }

  /**
   * Sets the delay interval between two consecutive exports. If unset, defaults to {@value
   * DEFAULT_SCHEDULE_DELAY_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setScheduleDelay(Duration delay) {
    requireNonNull(delay, "delay");
    return setScheduleDelay(delay.toNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * Sets the maximum time an export will be allowed to run before being cancelled. If unset,
   * defaults to {@value DEFAULT_EXPORT_TIMEOUT_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setExporterTimeout(long timeout, TimeUnit unit) {
    requireNonNull(unit, "unit");
    checkArgument(timeout >= 0, "timeout must be non-negative");
    exporterTimeoutNanos = unit.toNanos(timeout);
    return this;
  }

  /**
   * Sets the maximum time an export will be allowed to run before being cancelled. If unset,
   * defaults to {@value DEFAULT_EXPORT_TIMEOUT_MILLIS}ms.
   */
  public BatchSpanProcessorBuilder setExporterTimeout(Duration timeout) {
    requireNonNull(timeout, "timeout");
    return setExporterTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * Sets the maximum number of Spans that are kept in the queue before start dropping. More memory
   * than this value may be allocated to optimize queue access.
   *
   * <p>See the BatchSampledSpansProcessor class description for a high-level design description of
   * this class.
   *
   * <p>Default value is {@code 2048}.
   *
   * @param maxQueueSize the maximum number of Spans that are kept in the queue before start
   *     dropping.
   * @return this.
   * @see BatchSpanProcessorBuilder#DEFAULT_MAX_QUEUE_SIZE
   */
  public BatchSpanProcessorBuilder setMaxQueueSize(int maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
    return this;
  }

  /**
   * Sets the maximum batch size for every export. This must be smaller or equal to {@code
   * maxQueuedSpans}.
   *
   * <p>Default value is {@code 512}.
   *
   * @param maxExportBatchSize the maximum batch size for every export.
   * @return this.
   * @see BatchSpanProcessorBuilder#DEFAULT_MAX_EXPORT_BATCH_SIZE
   */
  public BatchSpanProcessorBuilder setMaxExportBatchSize(int maxExportBatchSize) {
    checkArgument(maxExportBatchSize > 0, "maxExportBatchSize must be positive.");
    this.maxExportBatchSize = maxExportBatchSize;
    return this;
  }

  /**
   * Returns a new {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor} that batches, then
   * converts spans to proto and forwards them to the given {@code spanExporter}.
   *
   * @return a new {@link io.opentelemetry.sdk.trace.export.BatchSpanProcessor}.
   * @throws NullPointerException if the {@code spanExporter} is {@code null}.
   */
  public BatchSpanProcessor build() {
    return new BatchSpanProcessor(
        spanExporter, scheduleDelayNanos, maxQueueSize, maxExportBatchSize, exporterTimeoutNanos);
  }
}
