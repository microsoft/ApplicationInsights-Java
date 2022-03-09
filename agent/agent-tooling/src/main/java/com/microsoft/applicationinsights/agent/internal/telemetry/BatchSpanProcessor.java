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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
      long exporterTimeoutNanos,
      int maxConcurrentExports,
      String queueName) {
    MpscArrayQueue<TelemetryItem> queue = new MpscArrayQueue<>(maxQueueSize);
    this.worker =
        new Worker(
            spanExporter,
            scheduleDelayNanos,
            maxExportBatchSize,
            exporterTimeoutNanos,
            maxConcurrentExports,
            queue,
            queue.capacity(),
            queueName);
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
    private final int maxConcurrentExports;

    private long nextExportTime;

    private final Queue<TelemetryItem> queue;
    private final int queueCapacity;
    private final String queueName;
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

    private static final OperationLogger queuingSpanLogger =
        new OperationLogger(BatchSpanProcessor.class, "Queuing span");

    private Worker(
        TelemetryChannel spanExporter,
        long scheduleDelayNanos,
        int maxExportBatchSize,
        long exporterTimeoutNanos,
        int maxConcurrentExports,
        Queue<TelemetryItem> queue,
        int queueCapacity,
        String queueName) {
      this.spanExporter = spanExporter;
      this.scheduleDelayNanos = scheduleDelayNanos;
      this.maxExportBatchSize = maxExportBatchSize;
      this.exporterTimeoutNanos = exporterTimeoutNanos;
      this.maxConcurrentExports = maxConcurrentExports;
      this.queue = queue;
      this.queueCapacity = queueCapacity;
      this.queueName = queueName;
      this.signal = new ArrayBlockingQueue<>(1);
      this.batch = new ArrayList<>(this.maxExportBatchSize);
    }

    private void addSpan(TelemetryItem span) {
      if (!queue.offer(span)) {
        queuingSpanLogger.recordFailure(
            "Max "
                + queueName
                + " export queue capacity of "
                + queueCapacity
                + " has been hit, dropping a telemetry record (max "
                + queueName
                + " export queue capacity can be increased in the applicationinsights.json"
                + " configuration file, e.g. { \"preview\": { \""
                + queueName
                + "ExportQueueCapacity\": "
                + (queueCapacity * 2)
                + " } }");
      } else {
        queuingSpanLogger.recordSuccess();
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
        List<CompletableResultCode> concurrentExports = new ArrayList<>();
        while (!queue.isEmpty() && concurrentExports.size() < maxConcurrentExports) {
          batch.add(queue.poll());
          if (batch.size() >= maxExportBatchSize) {
            concurrentExports.add(exportCurrentBatch());
          }
        }
        if (concurrentExports.isEmpty() && System.nanoTime() >= nextExportTime) {
          concurrentExports.add(exportCurrentBatch());
        }
        if (!concurrentExports.isEmpty()) {
          CompletableResultCode.ofAll(concurrentExports)
              .join(exporterTimeoutNanos, TimeUnit.NANOSECONDS);
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
          exportCurrentBatch().join(exporterTimeoutNanos, TimeUnit.NANOSECONDS);
        }
      }
      exportCurrentBatch().join(exporterTimeoutNanos, TimeUnit.NANOSECONDS);
      CompletableResultCode flushResult = this.flushRequested.get();
      if (flushResult != null) {
        flushResult.succeed();
        flushRequested.set(null);
      }
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

    private CompletableResultCode exportCurrentBatch() {
      if (batch.isEmpty()) {
        return CompletableResultCode.ofSuccess();
      }

      try {
        // batching, retry, logging, and writing to disk on failure occur downstream
        return spanExporter.send(Collections.unmodifiableList(batch));
      } finally {
        batch.clear();
      }
    }
  }
}
