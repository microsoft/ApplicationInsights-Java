// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static com.azure.monitor.opentelemetry.exporter.implementation.utils.AzureMonitorMsgId.BATCH_ITEM_PROCESSOR_ERROR;

import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryItemExporter;
import io.opentelemetry.internal.shaded.jctools.queues.MpscArrayQueue;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// copied from io.opentelemetry.sdk.trace.export.BatchSpanProcessor
public final class BatchItemProcessor {

  private static final String WORKER_THREAD_NAME =
      BatchItemProcessor.class.getSimpleName() + "_WorkerThread";

  private final Worker worker;
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  /**
   * Returns a new Builder for {@link BatchItemProcessor}.
   *
   * @param exporter the {@code TelemetryItemExporter} to where the telemetry items are pushed.
   * @return a new {@link BatchItemProcessor}.
   * @throws NullPointerException if the {@code exporter} is {@code null}.
   */
  public static BatchItemProcessorBuilder builder(TelemetryItemExporter exporter) {
    return new BatchItemProcessorBuilder(exporter);
  }

  BatchItemProcessor(
      TelemetryItemExporter exporter,
      long scheduleDelayNanos,
      int maxQueueSize,
      int maxExportBatchSize,
      long exporterTimeoutNanos,
      int maxPendingExports,
      String queueName) {
    MpscArrayQueue<TelemetryItem> queue = new MpscArrayQueue<>(maxQueueSize);
    this.worker =
        new Worker(
            exporter,
            scheduleDelayNanos,
            maxExportBatchSize,
            exporterTimeoutNanos,
            maxPendingExports,
            queue,
            queue.capacity(),
            queueName);
    Thread workerThread = new DaemonThreadFactory(WORKER_THREAD_NAME).newThread(worker);
    workerThread.start();
  }

  public void trackAsync(TelemetryItem item) {
    worker.addItem(item);
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

  // Worker is a thread that batches multiple items and calls the registered TelemetryItemExporter
  // to export the data.
  private static final class Worker implements Runnable {

    private final TelemetryItemExporter exporter;
    private final long scheduleDelayNanos;
    private final int maxExportBatchSize;
    private final long exporterTimeoutNanos;
    private final int maxPendingExports;

    private long nextExportTime;

    private final Queue<TelemetryItem> queue;
    private final int queueCapacity;
    private final String queueName;
    // When waiting on the items queue, exporter thread sets this atomic to the number of more
    // items it needs before doing an export. Writer threads would then wait for the queue to reach
    // itemsNeeded size before notifying the exporter thread about new entries.
    // Integer.MAX_VALUE is used to imply that exporter thread is not expecting any signal. Since
    // exporter thread doesn't expect any signal initially, this value is initialized to
    // Integer.MAX_VALUE.
    private final AtomicInteger itemsNeeded = new AtomicInteger(Integer.MAX_VALUE);
    private final BlockingQueue<Boolean> signal;
    private final AtomicReference<CompletableResultCode> flushRequested = new AtomicReference<>();
    private volatile boolean continueWork = true;
    private final ArrayList<TelemetryItem> batch;

    private final Set<CompletableResultCode> pendingExports =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final OperationLogger queuingItemLogger =
        new OperationLogger(BatchItemProcessor.class, "Queuing telemetry item");

    private static final OperationLogger addAsyncExport =
        new OperationLogger(BatchItemProcessor.class, "Add async export");

    private Worker(
        TelemetryItemExporter exporter,
        long scheduleDelayNanos,
        int maxExportBatchSize,
        long exporterTimeoutNanos,
        int maxPendingExports,
        Queue<TelemetryItem> queue,
        int queueCapacity,
        String queueName) {
      this.exporter = exporter;
      this.scheduleDelayNanos = scheduleDelayNanos;
      this.maxExportBatchSize = maxExportBatchSize;
      this.exporterTimeoutNanos = exporterTimeoutNanos;
      this.maxPendingExports = maxPendingExports;
      this.queue = queue;
      this.queueCapacity = queueCapacity;
      this.queueName = queueName;
      this.signal = new ArrayBlockingQueue<>(1);
      this.batch = new ArrayList<>(this.maxExportBatchSize);
    }

    private void addItem(TelemetryItem item) {
      if (!queue.offer(item)) {
        queuingItemLogger.recordFailure(
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
                + " } }",
            BATCH_ITEM_PROCESSOR_ERROR);
      } else {
        queuingItemLogger.recordSuccess();
        if (queue.size() >= itemsNeeded.get()) {
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
              itemsNeeded.set(maxExportBatchSize - batch.size());
              signal.poll(pollWaitTime, TimeUnit.NANOSECONDS);
              itemsNeeded.set(Integer.MAX_VALUE);
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    }

    private void flush() {
      int itemsToFlush = queue.size();
      while (itemsToFlush > 0) {
        TelemetryItem item = queue.poll();
        assert item != null;
        batch.add(item);
        itemsToFlush--;
        if (batch.size() >= maxExportBatchSize) {
          exportCurrentBatch();
        }
      }
      exportCurrentBatch();
      CompletableResultCode.ofAll(pendingExports).join(exporterTimeoutNanos, TimeUnit.NANOSECONDS);
      CompletableResultCode flushResult = flushRequested.get();
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
      CompletableResultCode overallResult = new CompletableResultCode();
      CompletableResultCode workerResult = forceFlushWorker();
      workerResult.whenComplete(
          () -> {
            if (!workerResult.isSuccess()) {
              overallResult.fail();
              return;
            }
            CompletableResultCode exporterResult = exporter.flush();
            exporterResult.whenComplete(
                () -> {
                  if (exporterResult.isSuccess()) {
                    overallResult.succeed();
                  } else {
                    overallResult.fail();
                  }
                });
          });
      return overallResult;
    }

    private CompletableResultCode forceFlushWorker() {
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
        CompletableResultCode result = exporter.send(Collections.unmodifiableList(batch));
        if (pendingExports.size() < maxPendingExports - 1) {
          addAsyncExport.recordSuccess();
          pendingExports.add(result);
          result.whenComplete(
              () -> {
                pendingExports.remove(result);
              });
        } else {
          // need conditional, otherwise this will always get logged when maxPendingExports is 1
          // (e.g. statsbeat)
          if (maxPendingExports > 1) {
            addAsyncExport.recordFailure(
                "Max number of concurrent exports "
                    + maxPendingExports
                    + " has been hit, may see some export throttling due to this",
                BATCH_ITEM_PROCESSOR_ERROR);
          }
          result.join(exporterTimeoutNanos, TimeUnit.NANOSECONDS);
        }
      } finally {
        batch.clear();
      }
    }
  }
}
