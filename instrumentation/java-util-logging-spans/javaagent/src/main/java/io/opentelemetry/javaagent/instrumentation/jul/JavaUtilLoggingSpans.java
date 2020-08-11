/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jul;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.api.config.Config;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

public class JavaUtilLoggingSpans {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(JavaUtilLoggingSpans.class);

  private static final Tracer TRACER =
      OpenTelemetry.getGlobalTracer("io.opentelemetry.auto.java-util-logging");

  private static final Formatter FORMATTER = new AccessibleFormatter();

  public static void capture(final Logger logger, final LogRecord logRecord) {

    Level level = logRecord.getLevel();
    if (!logger.isLoggable(level)) {
      // this is already checked in most cases, except if Logger.log(LogRecord) was called directly
      return;
    }
    if (level.intValue() < getThreshold().intValue()) {
      return;
    }

    Throwable t = logRecord.getThrown();
    Span span =
        TRACER
            .spanBuilder(FORMATTER.formatMessage(logRecord))
            .setAttribute("applicationinsights.internal.log", true)
            .setAttribute("applicationinsights.internal.log_level", level.getName())
            .setAttribute("applicationinsights.internal.logger_name", logger.getName())
            .startSpan();
    if (t != null) {
      span.setAttribute("applicationinsights.internal.log_error_stack", toString(t));
    }
    span.end();
  }

  private static String toString(final Throwable t) {
    StringWriter out = new StringWriter();
    t.printStackTrace(new PrintWriter(out));
    return out.toString();
  }

  private static Level getThreshold() {
    String level = Config.get().getProperty("otel.experimental.log.capture.threshold");
    if (level == null) {
      return Level.OFF;
    }
    switch (level.toUpperCase()) {
      case "OFF":
        return Level.OFF;
      case "FATAL":
      case "ERROR":
      case "SEVERE":
        return Level.SEVERE;
      case "WARN":
      case "WARNING":
        return Level.WARNING;
      case "INFO":
        return Level.INFO;
      case "CONFIG":
        return Level.CONFIG;
      case "DEBUG":
      case "FINE":
        return Level.FINE;
      case "FINER":
        return Level.FINER;
      case "TRACE":
      case "FINEST":
        return Level.FINEST;
      case "ALL":
        return Level.ALL;
      default:
        log.error("unexpected value for experimental.log.capture.threshold: {}", level);
        return Level.OFF;
    }
  }

  // this is just needed for calling formatMessage in abstract super class
  public static class AccessibleFormatter extends Formatter {

    @Override
    public String format(final LogRecord record) {
      throw new UnsupportedOperationException();
    }
  }
}
