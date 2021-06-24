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

package com.microsoft.applicationinsights.internal;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jctools.queues.MpscArrayQueue;

// copied from io.opentelemetry.sdk.trace.export.BatchSpanProcessor
public final class BatchSpanProcessor {

  private static final String WORKER_THREAD_NAME =
      BatchSpanProcessor.class.getSimpleName() + "_WorkerThread";

  private final Worker worker;
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  /**
   * Returns a new Builder for {@link BatchSpanProcessor}.
   *
   * @param spanExporter the {@code SpanExporter} to where the Spans are pushed.
   * @return a new {@link BatchSpanProcessor}.
   * @throws NullPointerException if the {@code spanExporter} is {@code null}.
   */
  public static BatchSpanProcessorBuilder builder(TelemetryChannel spanExporter) {
    return new BatchSpanProcessorBuilder(spanExporter);
  }

  BatchSpanProcessor(
      TelemetryChannel spanExporter,
      long scheduleDelayNanos,
      int maxQueueSize,
      int maxExportBatchSize,
      long exporterTimeoutNanos) {
    this.worker =
        new Worker(
            spanExporter,
            scheduleDelayNanos,
            maxExportBatchSize,
            exporterTimeoutNanos,
            new MpscArrayQueue<>(maxQueueSize));
    Thread workerThread = new DaemonThreadFactory(WORKER_THREAD_NAME).newThread(worker);
    workerThread.start();
  }

  public void trackAsync(TelemetryItem span) {
    worker.addSpan(span);
  }

  public CompletableResultCode shutdown() {
    if (isShutdown.getAndSet(true)) {
      return CompletableResultCode.ofSuccess();
    }
    return worker.shutdown();
  }

  public CompletableResultCode forceFlush() {
    return worker.forceFlush();
  }

  // Worker is a thread that batches multiple spans and calls the registered SpanExporter to export
  // the data.
  private static final class Worker implements Runnable {

    private final TelemetryChannel spanExporter;
    private final long scheduleDelayNanos;
    private final int maxExportBatchSize;
    private final long exporterTimeoutNanos;

    private long nextExportTime;

    private final Queue<TelemetryItem> queue;
    // When waiting on the spans queue, exporter thread sets this atomic to the number of more
    // spans it needs before doing an export. Writer threads would then wait for the queue to reach
    // spansNeeded size before notifying the exporter thread about new entries.
    // Integer.MAX_VALUE is used to imply that exporter thread is not expecting any signal. Since
    // exporter thread doesn't expect any signal initially, this value is initialized to
    // Integer.MAX_VALUE.
    private final AtomicInteger spansNeeded = new AtomicInteger(Integer.MAX_VALUE);
    private final BlockingQueue<Boolean> signal;
    private final AtomicReference<CompletableResultCode> flushRequested = new AtomicReference<>();
    private volatile boolean continueWork = true;
    private final ArrayList<TelemetryItem> batch;

    private Worker(
        TelemetryChannel spanExporter,
        long scheduleDelayNanos,
        int maxExportBatchSize,
        long exporterTimeoutNanos,
        Queue<TelemetryItem> queue) {
      this.spanExporter = spanExporter;
      this.scheduleDelayNanos = scheduleDelayNanos;
      this.maxExportBatchSize = maxExportBatchSize;
      this.exporterTimeoutNanos = exporterTimeoutNanos;
      this.queue = queue;
      this.signal = new ArrayBlockingQueue<>(1);
      this.batch = new ArrayList<>(this.maxExportBatchSize);
    }

    private void addSpan(TelemetryItem span) {
      if (queue.offer(span)) {
        // FIXME (trask) log dropped span
        // droppedSpans.add(1);
      } else {
        if (queue.size() >= spansNeeded.get()) {
          signal.offer(true);
        }
      }
    }

    @Override
    public void run() {
      updateNextExportTime();

      while (continueWork) {
        if (flushRequested.get() != null) {
          flush();
        }
        while (!queue.isEmpty() && batch.size() < maxExportBatchSize) {
          batch.add(queue.poll());
        }
        if (batch.size() >= maxExportBatchSize || System.nanoTime() >= nextExportTime) {
          exportCurrentBatch();
          updateNextExportTime();
        }
        if (queue.isEmpty()) {
          try {
            long pollWaitTime = nextExportTime - System.nanoTime();
            if (pollWaitTime > 0) {
              spansNeeded.set(maxExportBatchSize - batch.size());
              signal.poll(pollWaitTime, TimeUnit.NANOSECONDS);
              spansNeeded.set(Integer.MAX_VALUE);
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    }

    private void flush() {
      int spansToFlush = queue.size();
      while (spansToFlush > 0) {
        TelemetryItem span = queue.poll();
        assert span != null;
        batch.add(span);
        spansToFlush--;
        if (batch.size() >= maxExportBatchSize) {
          exportCurrentBatch();
        }
      }
      exportCurrentBatch();
      flushRequested.get().succeed();
      flushRequested.set(null);
    }

    private void updateNextExportTime() {
      nextExportTime = System.nanoTime() + scheduleDelayNanos;
    }

    private CompletableResultCode shutdown() {
      CompletableResultCode result = new CompletableResultCode();

      CompletableResultCode flushResult = forceFlush();
      flushResult.whenComplete(
          () -> {
            continueWork = false;
            if (!flushResult.isSuccess()) {
              result.fail();
            } else {
              result.succeed();
            }
          });

      return result;
    }

    private CompletableResultCode forceFlush() {
      CompletableResultCode flushResult = new CompletableResultCode();
      // we set the atomic here to trigger the worker loop to do a flush of the entire queue.
      if (flushRequested.compareAndSet(null, flushResult)) {
        signal.offer(true);
      }
      CompletableResultCode possibleResult = flushRequested.get();
      // there's a race here where the flush happening in the worker loop could complete before we
      // get what's in the atomic. In that case, just return success, since we know it succeeded in
      // the interim.
      return possibleResult == null ? CompletableResultCode.ofSuccess() : possibleResult;
    }

    private void exportCurrentBatch() {
      if (batch.isEmpty()) {
        return;
      }

      try {
        // batching, retry, logging, and writing to disk on failure occur downstream
        CompletableResultCode result = spanExporter.send(Collections.unmodifiableList(batch));
        result.join(exporterTimeoutNanos, TimeUnit.NANOSECONDS);
      } finally {
        batch.clear();
      }
    }
  }
}
