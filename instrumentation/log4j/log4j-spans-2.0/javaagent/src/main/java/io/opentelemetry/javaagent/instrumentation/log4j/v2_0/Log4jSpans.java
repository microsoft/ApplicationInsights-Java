/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.log4j.v2_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.api.config.Config;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.Message;
import org.slf4j.LoggerFactory;

public class Log4jSpans {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(Log4jSpans.class);

  private static final Tracer TRACER =
      GlobalOpenTelemetry.getTracer("io.opentelemetry.javaagent.log4j-2.0");

  public static void capture(
      final Logger logger, final Level level, final Message message, final Throwable t) {

    if (level.intLevel() > getThreshold().intLevel()) {
      return;
    }

    SpanBuilder builder =
        TRACER
            .spanBuilder(message.getFormattedMessage())
            .setAttribute("applicationinsights.internal.log", true)
            .setAttribute("applicationinsights.internal.log_level", level.toString())
            .setAttribute("applicationinsights.internal.logger_name", logger.getName());
    for (Map.Entry<String, String> entry : ThreadContext.getImmutableContext().entrySet()) {
      builder.setAttribute(entry.getKey(), entry.getValue());
    }
    Span span = builder.startSpan();
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
        return Level.FATAL;
      case "ERROR":
      case "SEVERE":
        return Level.ERROR;
      case "WARN":
      case "WARNING":
        return Level.WARN;
      case "INFO":
        return Level.INFO;
      case "CONFIG":
      case "DEBUG":
      case "FINE":
      case "FINER":
        return Level.DEBUG;
      case "TRACE":
      case "FINEST":
        return Level.TRACE;
      case "ALL":
        return Level.ALL;
      default:
        log.error("unexpected value for experimental.log.capture.threshold: {}", level);
        return Level.OFF;
    }
  }
}
