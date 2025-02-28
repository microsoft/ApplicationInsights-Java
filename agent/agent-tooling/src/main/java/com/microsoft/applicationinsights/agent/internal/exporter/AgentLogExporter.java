// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.exporter;

import static com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.AzureMonitorMsgId.EXPORTER_MAPPING_ERROR;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.LogDataMapper;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulse;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.sampling.AiFixedPercentageSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides;
import com.microsoft.applicationinsights.agent.internal.telemetry.BatchItemProcessor;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.ExceptionAttributes;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentLogExporter implements LogRecordExporter {

  private static final Logger logger = LoggerFactory.getLogger(AgentLogExporter.class);

  private static final OperationLogger exportingLogLogger =
      new OperationLogger(AgentLogExporter.class, "Exporting log");

  // TODO (trask) could implement this in a filtering LogExporter instead
  private volatile int severityThreshold;

  private final SamplingOverrides logSamplingOverrides;
  private final SamplingOverrides exceptionSamplingOverrides;
  private final LogDataMapper mapper;
  private final Consumer<TelemetryItem> telemetryItemConsumer;

  public AgentLogExporter(
      int severityThreshold,
      List<SamplingOverride> logSamplingOverrides,
      List<SamplingOverride> exceptionSamplingOverrides,
      LogDataMapper mapper,
      @Nullable QuickPulse quickPulse,
      BatchItemProcessor batchItemProcessor) {
    this.severityThreshold = severityThreshold;
    this.logSamplingOverrides = new SamplingOverrides(logSamplingOverrides);
    this.exceptionSamplingOverrides = new SamplingOverrides(exceptionSamplingOverrides);
    this.mapper = mapper;
    telemetryItemConsumer =
        telemetryItem -> {
          if (quickPulse != null) {
            try {
              logger.info("quickpulse.add from Log telemetryItemConsumer for {}", telemetryItem.getData().toJsonString());
            } catch (Exception e) {
              logger.error("failed to log telemetry item ", e);
            }

            quickPulse.add(telemetryItem);
          }
          TelemetryObservers.INSTANCE
              .getObservers()
              .forEach(consumer -> consumer.accept(telemetryItem));
          batchItemProcessor.trackAsync(telemetryItem);
        };
  }

  public void setSeverityThreshold(int severityThreshold) {
    this.severityThreshold = severityThreshold;
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
      int severityNumber = log.getSeverity().getSeverityNumber();
      if (severityNumber < severityThreshold) {
        return;
      }

      String stack = log.getAttributes().get(ExceptionAttributes.EXCEPTION_STACKTRACE);

      SamplingOverrides samplingOverrides =
          stack != null ? exceptionSamplingOverrides : logSamplingOverrides;

      SpanContext spanContext = log.getSpanContext();
      Double parentSpanSampleRate = log.getAttributes().get(AiSemanticAttributes.SAMPLE_RATE);

      AiFixedPercentageSampler sampler = samplingOverrides.getOverride(log.getAttributes());

      boolean hasSamplingOverride = sampler != null;

      if (!hasSamplingOverride
          && spanContext.isValid()
          && !spanContext.getTraceFlags().isSampled()) {
        // if there is no sampling override, and the log is part of an unsampled trace,
        // then don't capture it
        return;
      }

      Double sampleRate = null;
      if (hasSamplingOverride) {
        SamplingResult samplingResult = sampler.shouldSampleLog(spanContext, parentSpanSampleRate);
        if (samplingResult.getDecision() != SamplingDecision.RECORD_AND_SAMPLE) {
          logger.info("Sampling out log: {}", log.getBodyValue().asString());
          return;
        }
        sampleRate = samplingResult.getAttributes().get(AiSemanticAttributes.SAMPLE_RATE);
      }

      if (sampleRate == null) {
        sampleRate = parentSpanSampleRate;
      }

      logger.debug("exporting log: {}", log);

      // TODO (trask) no longer need to check AiSemanticAttributes.SAMPLE_RATE in map() method
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
