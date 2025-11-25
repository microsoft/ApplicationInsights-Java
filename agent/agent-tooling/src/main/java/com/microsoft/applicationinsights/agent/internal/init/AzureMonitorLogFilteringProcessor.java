// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.sampling.AiFixedPercentageSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides;
import com.microsoft.applicationinsights.agent.internal.processors.AgentProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.AttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.LogProcessor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.ExceptionAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AzureMonitorLogFilteringProcessor implements LogRecordProcessor {

  private final SamplingOverrides logSamplingOverrides;
  private final SamplingOverrides exceptionSamplingOverrides;
  private final LogRecordProcessor batchLogRecordProcessor;
  private final List<ProcessorApplier> previewProcessorAppliers;
  private final boolean hasPreviewProcessors;

  private volatile int severityThreshold;

  public AzureMonitorLogFilteringProcessor(
      List<Configuration.SamplingOverride> logSamplingOverrides,
      List<Configuration.SamplingOverride> exceptionSamplingOverrides,
      LogRecordProcessor batchLogRecordProcessor,
      int severityThreshold,
      List<ProcessorConfig> processorConfigs) {

    this.severityThreshold = severityThreshold;
    this.logSamplingOverrides = new SamplingOverrides(logSamplingOverrides);
    this.exceptionSamplingOverrides = new SamplingOverrides(exceptionSamplingOverrides);
    this.batchLogRecordProcessor = batchLogRecordProcessor;
    this.previewProcessorAppliers = buildProcessorAppliers(processorConfigs);
    this.hasPreviewProcessors = !previewProcessorAppliers.isEmpty();
  }

  public void setSeverityThreshold(int severityThreshold) {
    this.severityThreshold = severityThreshold;
  }

  @Override
  public void onEmit(Context context, ReadWriteLogRecord logRecord) {

    int severityNumber = logRecord.getSeverity().getSeverityNumber();
    if (severityNumber < severityThreshold) {
      // quick return
      return;
    }

    LogRecordData processedLogRecord =
        hasPreviewProcessors ? applyPreviewProcessors(logRecord) : logRecord.toLogRecordData();

    Double parentSpanSampleRate = null;
    Span currentSpan = Span.fromContext(context);
    if (currentSpan instanceof ReadableSpan) {
      ReadableSpan readableSpan = (ReadableSpan) currentSpan;
      parentSpanSampleRate = readableSpan.getAttribute(AiSemanticAttributes.SAMPLE_RATE);
    }

    // deal with sampling synchronously so that we only call setAttributeExceptionLogged()
    // when we know we are emitting the exception (span sampling happens synchronously as well)

    String stack =
        processedLogRecord
            .getAttributes()
            .get(ExceptionAttributes.EXCEPTION_STACKTRACE);

    SamplingOverrides samplingOverrides =
        stack != null ? exceptionSamplingOverrides : logSamplingOverrides;

    SpanContext spanContext = logRecord.getSpanContext();

    AiFixedPercentageSampler sampler = samplingOverrides.getOverride(processedLogRecord.getAttributes());

    boolean hasSamplingOverride = sampler != null;

    if (!hasSamplingOverride && spanContext.isValid() && !spanContext.getTraceFlags().isSampled()) {
      // if there is no sampling override, and the log is part of an unsampled trace,
      // then don't capture it
      return;
    }

    Double sampleRate = null;
    if (hasSamplingOverride) {
      SamplingResult samplingResult = sampler.shouldSampleLog(spanContext, parentSpanSampleRate);
      if (samplingResult.getDecision() != SamplingDecision.RECORD_AND_SAMPLE) {
        return;
      }
      sampleRate = samplingResult.getAttributes().get(AiSemanticAttributes.SAMPLE_RATE);
    }

    if (sampleRate == null) {
      sampleRate = parentSpanSampleRate;
    }

    if (sampleRate != null) {
      logRecord.setAttribute(AiSemanticAttributes.SAMPLE_RATE, sampleRate);
    }

    setAttributeExceptionLogged(LocalRootSpan.fromContext(context), stack);

    batchLogRecordProcessor.onEmit(context, logRecord);
  }

  @Override
  public CompletableResultCode shutdown() {
    return batchLogRecordProcessor.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return batchLogRecordProcessor.forceFlush();
  }

  @Override
  public void close() {
    batchLogRecordProcessor.close();
  }

  private LogRecordData applyPreviewProcessors(ReadWriteLogRecord logRecord) {
    LogRecordData processed = logRecord.toLogRecordData();
    for (ProcessorApplier processorApplier : previewProcessorAppliers) {
      processed = processorApplier.apply(processed);
    }
    return processed;
  }

  private static List<ProcessorApplier> buildProcessorAppliers(
      List<ProcessorConfig> processorConfigs) {
    if (processorConfigs.isEmpty()) {
      return Collections.emptyList();
    }
    List<ProcessorApplier> appliers = new ArrayList<>(processorConfigs.size());
    for (ProcessorConfig processorConfig : processorConfigs) {
      appliers.add(ProcessorApplier.fromConfig(processorConfig));
    }
    return Collections.unmodifiableList(appliers);
  }

  private static void setAttributeExceptionLogged(Span span, String stacktrace) {
    if (stacktrace != null) {
      span.setAttribute(AiSemanticAttributes.LOGGED_EXCEPTION, stacktrace);
    }
  }

  private interface ProcessorApplier {
    LogRecordData apply(LogRecordData logRecordData);

    static ProcessorApplier fromConfig(ProcessorConfig config) {
      config.validate();
      if (config.type == ProcessorType.ATTRIBUTE) {
        AttributeProcessor attributeProcessor = AttributeProcessor.create(config, true);
        return new AttributeProcessorApplier(attributeProcessor);
      }
      if (config.type == ProcessorType.LOG) {
        LogProcessor logProcessor = LogProcessor.create(config);
        return new LogBodyProcessorApplier(logProcessor);
      }
      throw new IllegalStateException("Unsupported processor type: " + config.type);
    }
  }

  private static final class AttributeProcessorApplier implements ProcessorApplier {
    private final AttributeProcessor attributeProcessor;

    private AttributeProcessorApplier(AttributeProcessor attributeProcessor) {
      this.attributeProcessor = attributeProcessor;
    }

    @Override
    public LogRecordData apply(LogRecordData logRecordData) {
      AgentProcessor.IncludeExclude include = attributeProcessor.getInclude();
      if (include != null
          && !include.isMatch(logRecordData.getAttributes(), logRecordData.getBody().asString())) {
        return logRecordData;
      }
      AgentProcessor.IncludeExclude exclude = attributeProcessor.getExclude();
      if (exclude != null
          && exclude.isMatch(logRecordData.getAttributes(), logRecordData.getBody().asString())) {
        return logRecordData;
      }
      return attributeProcessor.processActions(logRecordData);
    }
  }

  private static final class LogBodyProcessorApplier implements ProcessorApplier {
    private final LogProcessor logProcessor;

    private LogBodyProcessorApplier(LogProcessor logProcessor) {
      this.logProcessor = logProcessor;
    }

    @Override
    public LogRecordData apply(LogRecordData logRecordData) {
      AgentProcessor.IncludeExclude include = logProcessor.getInclude();
      if (include != null
          && !include.isMatch(logRecordData.getAttributes(), logRecordData.getBody().asString())) {
        return logRecordData;
      }
      AgentProcessor.IncludeExclude exclude = logProcessor.getExclude();
      if (exclude != null
          && exclude.isMatch(logRecordData.getAttributes(), logRecordData.getBody().asString())) {
        return logRecordData;
      }

      LogRecordData updatedLog = logProcessor.processFromAttributes(logRecordData);
      return logProcessor.processToAttributes(updatedLog);
    }
  }
}
