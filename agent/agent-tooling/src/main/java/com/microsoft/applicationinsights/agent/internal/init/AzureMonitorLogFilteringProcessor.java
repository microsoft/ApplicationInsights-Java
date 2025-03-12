// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.sampling.AiFixedPercentageSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.internal.AttributesMap;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.ExceptionAttributes;
import java.lang.reflect.Field;
import java.util.List;
import javax.annotation.Nullable;

public class AzureMonitorLogFilteringProcessor implements LogRecordProcessor {

  private static final ClientLogger logger = new ClientLogger(AzureMonitorLogProcessor.class);
  private static final Field lockField;
  private static final Field attributesMapField;

  static {
    Class<?> sdkReadWriteLogRecordClass = getSdkReadWriteLogRecordClass();
    lockField = getLockField(sdkReadWriteLogRecordClass);
    attributesMapField = getAttributesMapField(sdkReadWriteLogRecordClass);
  }

  private final SamplingOverrides logSamplingOverrides;
  private final SamplingOverrides exceptionSamplingOverrides;
  private final BatchLogRecordProcessor batchLogRecordProcessor;

  private volatile int severityThreshold;

  public AzureMonitorLogFilteringProcessor(
      List<Configuration.SamplingOverride> logSamplingOverrides,
      List<Configuration.SamplingOverride> exceptionSamplingOverrides,
      BatchLogRecordProcessor batchLogRecordProcessor,
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

  @Nullable
  private static Class<?> getSdkReadWriteLogRecordClass() {
    try {
      return Class.forName("io.opentelemetry.sdk.logs.SdkReadWriteLogRecord");
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  @Nullable
  private static Field getLockField(Class<?> sdkReadWriteLogRecordClass) {
    if (sdkReadWriteLogRecordClass == null) {
      return null;
    }
    try {
      Field lockField = sdkReadWriteLogRecordClass.getDeclaredField("lock");
      lockField.setAccessible(true);
      return lockField;
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  @Nullable
  private static Field getAttributesMapField(Class<?> sdkReadWriteLogRecordClass) {
    if (sdkReadWriteLogRecordClass == null) {
      return null;
    }
    try {
      Field attributesMapField = sdkReadWriteLogRecordClass.getDeclaredField("attributes");
      attributesMapField.setAccessible(true);
      return attributesMapField;
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  private static void setAttributeExceptionLogged(Span span, ReadWriteLogRecord logRecord) {
    if (lockField == null || attributesMapField == null) {
      return;
    }
    String stacktrace = null;
    try {
      synchronized (lockField) {
        // TODO add `getAttribute()` to `ReadWriteLogRecord` upstream
        stacktrace =
            ((AttributesMap) attributesMapField.get(logRecord))
                .get(ExceptionAttributes.EXCEPTION_STACKTRACE);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    if (stacktrace != null) {
      span.setAttribute(AiSemanticAttributes.LOGGED_EXCEPTION, stacktrace);
    }
  }
}
