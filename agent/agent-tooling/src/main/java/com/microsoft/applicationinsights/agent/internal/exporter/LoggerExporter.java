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

import com.azure.core.util.CoreUtils;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.ExceptionTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.Exceptions;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.logging.OperationLogger;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.SeverityLevel;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.TelemetryUtil;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.Severity;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collection;
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

  public LoggerExporter(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  @Override
  public CompletableResultCode export(Collection<LogData> logs) {
    if (CoreUtils.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      logger.debug("Instrumentation key is null or empty. Fail to export logs.");
      return CompletableResultCode.ofFailure();
    }

    boolean failure = false;
    for (LogData log : logs) {
      logger.debug("exporting log: {}", log);
      try {
        internalExport(log);
        exportingLogLogger.recordSuccess();
      } catch (Throwable t) {
        exportingLogLogger.recordFailure(t.getMessage(), t);
        failure = true;
      }
    }

    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    return failure ? CompletableResultCode.ofFailure() : CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  private void internalExport(LogData log) {
    int severity = log.getSeverity().getSeverityNumber();
    int threshold = getThreshold().getSeverityNumber();
    if (severity < threshold) {
      return;
    }

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

    telemetryBuilder.setSeverityLevel(toSeverityLevel(log.getSeverity()));
    String body = log.getBody().asString();
    if (body.isEmpty()) {
      body = "<empty>"; // breeze doesn't accept empty log messages
    }
    telemetryBuilder.setMessage(body);

    // set message-specific properties
    setLoggerProperties(
        telemetryBuilder,
        log.getInstrumentationLibraryInfo().getName(),
        attributes.get(SemanticAttributes.THREAD_NAME),
        log.getSeverity());

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

    telemetryBuilder.setExceptions(Exceptions.minimalParse(stack));
    telemetryBuilder.setSeverityLevel(toSeverityLevel(log.getSeverity()));

    // set exception-specific properties
    setLoggerProperties(
        telemetryBuilder,
        log.getInstrumentationLibraryInfo().getName(),
        attributes.get(SemanticAttributes.THREAD_NAME),
        log.getSeverity());

    if (log.getBody() != null) {
      telemetryBuilder.addProperty("Logger Message", log.getBody().asString());
    }
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

  private static final String LOG4J1_2_MDC_PREFIX = "log4j.mdc.";
  private static final String LOG4J2_CONTEXT_DATA_PREFIX = "log4j.context_data.";
  private static final String LOGBACK_MDC_PREFIX = "logback.mdc.";

  static void setExtraAttributes(AbstractTelemetryBuilder telemetryBuilder, Attributes attributes) {
    attributes.forEach(
        (key, value) -> {
          String stringKey = key.getKey();
          if (stringKey.startsWith(LOG4J2_CONTEXT_DATA_PREFIX)) {
            telemetryBuilder.addProperty(
                stringKey.substring(LOG4J2_CONTEXT_DATA_PREFIX.length()), String.valueOf(value));
            return;
          }
          if (stringKey.startsWith(LOGBACK_MDC_PREFIX)) {
            telemetryBuilder.addProperty(
                stringKey.substring(LOGBACK_MDC_PREFIX.length()), String.valueOf(value));
            return;
          }
          if (stringKey.startsWith(LOG4J1_2_MDC_PREFIX)) {
            telemetryBuilder.addProperty(
                stringKey.substring(LOG4J1_2_MDC_PREFIX.length()), String.valueOf(value));
            return;
          }
          if (stringKey.equals(AI_OPERATION_NAME_KEY.getKey())) {
            return;
          }
          if (stringKey.startsWith("thread.")) {
            return;
          }
          if (stringKey.startsWith("exception.")) {
            return;
          }
          String val = Exporter.convertToString(value, key.getType());
          if (val != null) {
            telemetryBuilder.addProperty(key.getKey(), val);
          }
        });
  }

  private static void setLoggerProperties(
      AbstractTelemetryBuilder telemetryBuilder,
      String loggerName,
      String threadName,
      Severity severity) {

    telemetryBuilder.addProperty("SourceType", "Logger");

    String loggingLevel = mapSeverityToLoggingLevel(severity);
    if (loggingLevel != null) {
      telemetryBuilder.addProperty("LoggingLevel", loggingLevel);
    }
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

  private static Severity getThreshold() {
    String severity = Config.get().getString("otel.experimental.log.capture.threshold");
    if (severity == null) {
      return Severity.UNDEFINED_SEVERITY_NUMBER;
    }
    switch (severity.toUpperCase()) {
      case "OFF":
        return Severity.UNDEFINED_SEVERITY_NUMBER;
      case "FATAL":
      case "ERROR":
      case "SEVERE":
        return Severity.ERROR;
      case "WARN":
      case "WARNING":
        return Severity.WARN;
      case "INFO":
        return Severity.INFO;
      case "CONFIG":
      case "DEBUG":
      case "FINE":
      case "FINER":
        return Severity.DEBUG;
      case "TRACE":
      case "FINEST":
      case "ALL":
        return Severity.TRACE;
      default:
        logger.error("unexpected value for otel.experimental.log.capture.threshold: {}", severity);
        return Severity.UNDEFINED_SEVERITY_NUMBER;
    }
  }

  // TODO need to retrieve logging frameworks' name (Log4j, Logback, Java Util Logging) so that we
  // can correctly map Severity to logging level
  @Nullable
  private static String mapSeverityToLoggingLevel(Severity severity) {
    switch (severity) {
      case UNDEFINED_SEVERITY_NUMBER:
        return null;
      case FATAL:
      case FATAL2:
      case FATAL3:
      case FATAL4:
        return "FATAL";
      case ERROR:
      case ERROR2:
      case ERROR3:
      case ERROR4:
        return "ERROR";
      case WARN:
      case WARN2:
      case WARN3:
      case WARN4:
        return "WARN";
      case INFO:
      case INFO2:
      case INFO3:
      case INFO4:
        return "INFO";
      case DEBUG:
      case DEBUG2:
      case DEBUG3:
      case DEBUG4:
        return "DEBUG";
      case TRACE:
      case TRACE2:
      case TRACE3:
      case TRACE4:
        return "TRACE";
      default:
        logger.error("Unexpected severity {}", severity);
        return null;
    }
  }
}
