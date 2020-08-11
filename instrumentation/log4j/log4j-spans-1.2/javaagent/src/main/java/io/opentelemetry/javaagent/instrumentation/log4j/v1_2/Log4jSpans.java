/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.log4j.v1_2;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.api.config.Config;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Map;
import org.apache.log4j.Category;
import org.apache.log4j.MDC;
import org.apache.log4j.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4jSpans {

  private static final Logger log = LoggerFactory.getLogger(Log4jSpans.class);

  private static final Tracer TRACER =
      OpenTelemetry.getGlobalTracer("io.opentelemetry.auto.log4j-1.1");

  // these constants are copied from org.apache.log4j.Priority and org.apache.log4j.Level because
  // Level was only introduced in 1.2, and then Level.TRACE was only introduced in 1.2.12
  private static final int OFF_INT = Integer.MAX_VALUE;
  private static final int FATAL_INT = 50000;
  private static final int ERROR_INT = 40000;
  private static final int WARN_INT = 30000;
  private static final int INFO_INT = 20000;
  private static final int DEBUG_INT = 10000;
  private static final int TRACE_INT = 5000;
  private static final int ALL_INT = Integer.MIN_VALUE;

  public static void capture(
      final Category logger, final Priority level, final Object message, final Throwable t) {

    if (level.toInt() < getThreshold()) {
      return;
    }

    SpanBuilder builder =
        TRACER
            .spanBuilder(String.valueOf(message))
            .setAttribute("applicationinsights.internal.log", true)
            .setAttribute("applicationinsights.internal.log_level", level.toString())
            .setAttribute("applicationinsights.internal.logger_name", logger.getName());
    Hashtable<?, ?> context = MDC.getContext();
    if (context != null) {
      for (Map.Entry<?, ?> entry : context.entrySet()) {
        builder.setAttribute(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }
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

  private static int getThreshold() {
    String level = Config.get().getProperty("otel.experimental.log.capture.threshold");
    if (level == null) {
      return OFF_INT;
    }
    switch (level.toUpperCase()) {
      case "OFF":
        return OFF_INT;
      case "FATAL":
        return FATAL_INT;
      case "ERROR":
      case "SEVERE":
        return ERROR_INT;
      case "WARN":
      case "WARNING":
        return WARN_INT;
      case "INFO":
        return INFO_INT;
      case "CONFIG":
      case "DEBUG":
      case "FINE":
      case "FINER":
        return DEBUG_INT;
      case "TRACE":
      case "FINEST":
        return TRACE_INT;
      case "ALL":
        return ALL_INT;
      default:
        log.error("unexpected value for experimental.log.capture.threshold: {}", level);
        return OFF_INT;
    }
  }
}
