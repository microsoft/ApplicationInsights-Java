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

package com.microsoft.applicationinsights.agent.internal.exporter;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.ExceptionTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.Exceptions;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.SeverityLevel;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.Severity;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerExporter implements LogExporter {

  private static final Logger logger = LoggerFactory.getLogger(LoggerExporter.class);

  private static final AttributeKey<String> AI_OPERATION_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operation_name");

  private static final OperationLogger exportingLogLogger =
      new OperationLogger(Exporter.class, "Exporting log");

  private final TelemetryClient telemetryClient;
  private final AtomicBoolean stopped = new AtomicBoolean();

  public LoggerExporter(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  @Override
  public CompletableResultCode export(Collection<LogData> logs) {

    // TODO (heya) ignore logs that do not meet the configured logging threshold
    //  (configuration.instrumentation.logging.level)
    //  this used to be handled in the instrumentation, but otel instrumentation doesn't have that
    //  setting yet, which is ok we should be able to filter here fine

    if (Strings.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      logger.debug("Instrumentation key is null or empty. Fail to export logs.");
      return CompletableResultCode.ofFailure();
    }

    if (stopped.get()) {
      logger.debug("LoggerExporter has been stopped. Fail to export logs.");
      return CompletableResultCode.ofFailure();
    }

    boolean failure = false;
    for (LogData log : logs) {
      logger.debug("exporting log: {}", log); // do we need to log this, will that be too much?
      try {
        internalExport(log);
        exportingLogLogger.recordSuccess();
      } catch (Throwable t) {
        exportingLogLogger.recordFailure(t.getMessage(), t);
        failure = true;
      }
    }

    return failure ? CompletableResultCode.ofFailure() : CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    stopped.set(true);
    return CompletableResultCode.ofSuccess();
  }

  private void internalExport(LogData log) {
    // note: instrumentationLibraryInfo name is the logger name, not the instrumentation name, so
    // cannot use it for instrumentation statsbeat

    String stack = log.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);
    if (stack == null) {
      trackMessage(log);
    } else {
      trackMessageAsException(log, stack);
    }
  }

  private void trackMessage(LogData log) {
    MessageTelemetryBuilder telemetryBuilder = telemetryClient.newMessageTelemetryBuilder();

    Attributes attributes = log.getAttributes();

    // set standard properties
    telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochNanos(log.getEpochNanos()));
    setOperationTags(telemetryBuilder, log);
    setSampleRate(telemetryBuilder, log);
    setExtraAttributes(telemetryBuilder, attributes);

    // set message-specific properties
    String loggerName = log.getInstrumentationLibraryInfo().getName();
    String threadName = attributes.get(SemanticAttributes.THREAD_NAME);

    telemetryBuilder.setSeverityLevel(toSeverityLevel(log.getSeverity()));
    telemetryBuilder.setMessage(log.getBody().asString());

    setLoggerProperties(telemetryBuilder, loggerName, threadName);

    // export
    telemetryClient.trackAsync(telemetryBuilder.build());
  }

  private void trackMessageAsException(LogData log, String stack) {
    ExceptionTelemetryBuilder telemetryBuilder = telemetryClient.newExceptionTelemetryBuilder();
    Attributes attributes = log.getAttributes();

    // set standard properties
    setOperationTags(telemetryBuilder, log);
    telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochNanos(log.getEpochNanos()));
    setSampleRate(telemetryBuilder, log);
    setExtraAttributes(telemetryBuilder, attributes);

    // set exception-specific properties
    String loggerName = log.getInstrumentationLibraryInfo().getName();
    String threadName = attributes.get(SemanticAttributes.THREAD_NAME);

    telemetryBuilder.setExceptions(Exceptions.minimalParse(stack));
    telemetryBuilder.setSeverityLevel(toSeverityLevel(log.getSeverity()));
    telemetryBuilder.addProperty("Logger Message", log.getBody().asString());
    telemetryBuilder.addProperty("SourceType", "Logger");
    setLoggerProperties(telemetryBuilder, loggerName, threadName);

    // export
    telemetryClient.trackAsync(telemetryBuilder.build());
  }

  private static void setOperationTags(AbstractTelemetryBuilder telemetryBuilder, LogData log) {
    telemetryBuilder.addTag(
        ContextTagKeys.AI_OPERATION_ID.toString(), log.getSpanContext().getTraceId());
    setOperationParentId(telemetryBuilder, log.getSpanContext().getSpanId());
    setOperationName(telemetryBuilder, log.getAttributes());
  }

  private static void setOperationParentId(
      AbstractTelemetryBuilder telemetryBuilder, String parentSpanId) {
    if (SpanId.isValid(parentSpanId)) {
      telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), parentSpanId);
    }
  }

  private static void setOperationName(
      AbstractTelemetryBuilder telemetryBuilder, Attributes attributes) {
    String operationName = attributes.get(AI_OPERATION_NAME_KEY);
    if (operationName != null) {
      telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_NAME.toString(), operationName);
    }
  }

  private static void setSampleRate(AbstractTelemetryBuilder telemetryBuilder, LogData log) {
    float samplingPercentage =
        TelemetryUtil.getSamplingPercentage(log.getSpanContext().getTraceState(), 10, true);
    if (samplingPercentage != 100) {
      telemetryBuilder.setSampleRate(samplingPercentage);
    }
  }

  private static final String LOG4J_CONTEXT_DATA_PREFIX = "log4j.context_data.";
  private static final String LOGBACK_MDC_PREFIX = "logback.mdc.";

  static void setExtraAttributes(AbstractTelemetryBuilder telemetryBuilder, Attributes attributes) {
    attributes.forEach(
        (key, value) -> {
          String stringKey = key.getKey();
          if (stringKey.startsWith(LOG4J_CONTEXT_DATA_PREFIX)) {
            telemetryBuilder.addProperty(
                stringKey.substring(LOG4J_CONTEXT_DATA_PREFIX.length()), String.valueOf(value));
            return;
          }
          if (stringKey.startsWith(LOGBACK_MDC_PREFIX)) {
            telemetryBuilder.addProperty(
                stringKey.substring(LOGBACK_MDC_PREFIX.length()), String.valueOf(value));
            return;
          }
        });
  }

  private static void setLoggerProperties(
      AbstractTelemetryBuilder telemetryBuilder, String loggerName, String threadName) {
    if (loggerName != null) {
      telemetryBuilder.addProperty("LoggerName", loggerName);
    }
    if (threadName != null) {
      telemetryBuilder.addProperty("ThreadName", threadName);
    }
  }

  @Nullable
  private static SeverityLevel toSeverityLevel(Severity severity) {
    if (severity == null) {
      return null;
    }
    switch (severity) {
      case UNDEFINED_SEVERITY_NUMBER:
        // TODO (trask) AI mapping: is this a good fallback?
      case TRACE:
      case TRACE2:
      case TRACE3:
      case TRACE4:
      case DEBUG:
      case DEBUG2:
      case DEBUG3:
      case DEBUG4:
        return SeverityLevel.VERBOSE;
      case INFO:
      case INFO2:
      case INFO3:
      case INFO4:
        return SeverityLevel.INFORMATION;
      case WARN:
      case WARN2:
      case WARN3:
      case WARN4:
        return SeverityLevel.WARNING;
      case ERROR:
      case ERROR2:
      case ERROR3:
      case ERROR4:
        return SeverityLevel.ERROR;
      case FATAL:
      case FATAL2:
      case FATAL3:
      case FATAL4:
        return SeverityLevel.CRITICAL;
    }
    // TODO (trask) AI mapping: is this a good fallback?
    return SeverityLevel.VERBOSE;
  }
}
