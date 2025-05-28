// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.sampling.AiFixedPercentageSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.ExceptionAttributes;
import java.util.List;

public class AzureMonitorLogFilteringProcessor implements LogRecordProcessor {

  private final SamplingOverrides logSamplingOverrides;
  private final SamplingOverrides exceptionSamplingOverrides;
  private final LogRecordProcessor batchLogRecordProcessor;

  private volatile int severityThreshold;

  public AzureMonitorLogFilteringProcessor(
      List<Configuration.SamplingOverride> logSamplingOverrides,
      List<Configuration.SamplingOverride> exceptionSamplingOverrides,
      LogRecordProcessor batchLogRecordProcessor,
      int severityThreshold) {

    this.severityThreshold = severityThreshold;
    this.logSamplingOverrides = new SamplingOverrides(logSamplingOverrides);
    this.exceptionSamplingOverrides = new SamplingOverrides(exceptionSamplingOverrides);
    this.batchLogRecordProcessor = batchLogRecordProcessor;
    this.severityThreshold = severityThreshold;
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

    Double parentSpanSampleRate = null;
    Span currentSpan = Span.fromContext(context);
    if (currentSpan instanceof ReadableSpan) {
      ReadableSpan readableSpan = (ReadableSpan) currentSpan;
      parentSpanSampleRate = readableSpan.getAttribute(AiSemanticAttributes.SAMPLE_RATE);
    }

    // deal with sampling synchronously so that we only call setAttributeExceptionLogged()
    // when we know we are emitting the exception (span sampling happens synchronously as well)

    String stack = logRecord.getAttribute(ExceptionAttributes.EXCEPTION_STACKTRACE);

    SamplingOverrides samplingOverrides =
        stack != null ? exceptionSamplingOverrides : logSamplingOverrides;

    SpanContext spanContext = logRecord.getSpanContext();

    AiFixedPercentageSampler sampler = samplingOverrides.getOverride(logRecord.getAttributes());

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

    setAttributeExceptionLogged(LocalRootSpan.fromContext(context), logRecord);

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

  private static void setAttributeExceptionLogged(Span span, ReadWriteLogRecord logRecord) {
    String stacktrace = logRecord.getAttribute(ExceptionAttributes.EXCEPTION_STACKTRACE);
    if (stacktrace != null) {
      span.setAttribute(AiSemanticAttributes.LOGGED_EXCEPTION, stacktrace);
    }
  }
}
