// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import static com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.AzureMonitorMsgId.EXPORTER_MAPPING_ERROR;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulse;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.semconv.ExceptionAttributes;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentLogExporter implements LogRecordExporter {

  private static final Logger logger = LoggerFactory.getLogger(AgentLogExporter.class);

  private static final OperationLogger exportingLogLogger =
      new OperationLogger(AgentLogExporter.class, "Exporting log");

  private final LogDataMapper mapper;
  private final Consumer<TelemetryItem> telemetryItemConsumer;

  public AgentLogExporter(
      LogDataMapper mapper,
      @Nullable QuickPulse quickPulse,
      BatchItemProcessor batchItemProcessor) {
    this.mapper = mapper;
    telemetryItemConsumer =
        telemetryItem -> {
          if (quickPulse != null) {
            quickPulse.add(telemetryItem);
          }
          TelemetryObservers.INSTANCE
              .getObservers()
              .forEach(consumer -> consumer.accept(telemetryItem));
          batchItemProcessor.trackAsync(telemetryItem);
        };
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    // incrementing CallDepth for LoggerProvider causes the OpenTelemetry Java agent logging
    // instrumentation to back off
    //
    // note: recursive log capture is only known to be an issue on Wildfly, because it redirects
    // System.out back to a logging library which is itself instrumented by the Java agent
    //
    // see OutOfMemoryWithDebugLevelTest for repro that will fail without this code
    CallDepth callDepth = CallDepth.forClass(LoggerProvider.class);
    callDepth.getAndIncrement();
    try {
      return internalExport(logs);
    } finally {
      callDepth.decrementAndGet();
    }
  }

  private CompletableResultCode internalExport(Collection<LogRecordData> logs) {
    if (TelemetryClient.getActive().getConnectionString() == null) {
      // Azure Functions consumption plan
      logger.debug("Instrumentation key is null or empty. Fail to export logs.");
      return CompletableResultCode.ofFailure();
    }
    for (LogRecordData log : logs) {
      internalExport(log);
    }
    // always returning success, because all error handling is performed internally
    return CompletableResultCode.ofSuccess();
  }

  private void internalExport(LogRecordData log) {
    try {
      logger.debug("exporting log: {}", log);

      String stack = log.getAttributes().get(ExceptionAttributes.EXCEPTION_STACKTRACE);
      Double sampleRate = log.getAttributes().get(AiSemanticAttributes.SAMPLE_RATE);

      // TODO (trask) get stack and sampleRate inside map() method instead of passing into
      TelemetryItem telemetryItem = mapper.map(log, stack, sampleRate);
      telemetryItemConsumer.accept(telemetryItem);

      exportingLogLogger.recordSuccess();
    } catch (Throwable t) {
      exportingLogLogger.recordFailure(t.getMessage(), t, EXPORTER_MAPPING_ERROR);
    }
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }
}
