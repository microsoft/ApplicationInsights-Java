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
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerExporter implements LogExporter {

  private static final AttributeKey<String> AI_OPERATION_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operation_name");
  private static final AttributeKey<String> AI_LOG_LEVEL_KEY =
      AttributeKey.stringKey("applicationinsights.internal.log_level");
  private static final AttributeKey<String> AI_LOGGER_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.logger_name");
  private static final Logger logger = LoggerFactory.getLogger(LoggerExporter.class);
  private static final OperationLogger exportingLogLogger =
      new OperationLogger(Exporter.class, "Exporting log");
  private final TelemetryClient telemetryClient;
  private final AtomicBoolean stopped = new AtomicBoolean();

  public LoggerExporter(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  @Override
  public CompletableResultCode export(Collection<LogData> logs) {
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
    String instrumentationName = log.getInstrumentationLibraryInfo().getName();
    telemetryClient
        .getStatsbeatModule()
        .getInstrumentationStatsbeat()
        .addInstrumentation(instrumentationName);

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
    ExporterUtil.setExtraAttributes(telemetryBuilder, attributes, logger);

    // set message-specific properties
    String level = attributes.get(AI_LOG_LEVEL_KEY);
    String loggerName = attributes.get(AI_LOGGER_NAME_KEY);
    String threadName = attributes.get(SemanticAttributes.THREAD_NAME);

    telemetryBuilder.setSeverityLevel(toSeverityLevel(level));
    telemetryBuilder.setMessage(log.getName());

    setLoggerProperties(telemetryBuilder, level, loggerName, threadName);

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
    ExporterUtil.setExtraAttributes(telemetryBuilder, attributes, logger);

    // set exception-specific properties
    String level = attributes.get(AI_LOG_LEVEL_KEY);
    String loggerName = attributes.get(AI_LOGGER_NAME_KEY);
    String threadName = attributes.get(SemanticAttributes.THREAD_NAME);

    telemetryBuilder.setExceptions(Exceptions.minimalParse(stack));
    telemetryBuilder.setSeverityLevel(toSeverityLevel(level));
    telemetryBuilder.addProperty("Logger Message", log.getName());
    setLoggerProperties(telemetryBuilder, level, loggerName, threadName);

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

  private static void setLoggerProperties(
      AbstractTelemetryBuilder telemetryBuilder,
      String level,
      String loggerName,
      String threadName) {
    if (level != null) {
      // TODO are these needed? level is already reported as severityLevel, sourceType maybe needed
      // for exception telemetry only?
      telemetryBuilder.addProperty("SourceType", "Logger");
      telemetryBuilder.addProperty("LoggingLevel", level);
    }
    if (loggerName != null) {
      telemetryBuilder.addProperty("LoggerName", loggerName);
    }
    if (threadName != null) {
      telemetryBuilder.addProperty("ThreadName", threadName);
    }
  }

  @Nullable
  private static SeverityLevel toSeverityLevel(String level) {
    if (level == null) {
      return null;
    }
    switch (level) {
      case "FATAL":
        return SeverityLevel.CRITICAL;
      case "ERROR":
      case "SEVERE":
        return SeverityLevel.ERROR;
      case "WARN":
      case "WARNING":
        return SeverityLevel.WARNING;
      case "INFO":
        return SeverityLevel.INFORMATION;
      case "DEBUG":
      case "TRACE":
      case "CONFIG":
      case "FINE":
      case "FINER":
      case "FINEST":
      case "ALL":
        return SeverityLevel.VERBOSE;
      default:
        // TODO (trask) AI mapping: is this a good fallback?
        logger.debug("Unexpected level {}, using VERBOSE level as default", level);
        return SeverityLevel.VERBOSE;
    }
  }
}
