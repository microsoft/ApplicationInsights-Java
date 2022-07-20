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

package com.azure.monitor.opentelemetry.exporter.implementation.utils;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.ExceptionDetailBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.StackFrameBuilder;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.instrumentation.api.internal.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

// naming convention:
// * MonitorDomain data
// * TelemetryItem telemetry
public class TelemetryUtil {

  private static final int MAX_PARSED_STACK_LENGTH =
      32768; // Breeze will reject parsedStack exceeding 65536 bytes. Each char is 2 bytes long.

  public static List<ExceptionDetailBuilder> getExceptions(Throwable throwable) {
    List<ExceptionDetailBuilder> exceptions = new ArrayList<>();
    convertExceptionTree(throwable, null, exceptions, Integer.MAX_VALUE);
    return exceptions;
  }

  private static void convertExceptionTree(
      Throwable exception,
      @Nullable ExceptionDetailBuilder parentExceptionDetails,
      List<ExceptionDetailBuilder> exceptions,
      int stackSize) {
    if (exception == null) {
      exception = new Exception("");
    }

    if (stackSize == 0) {
      return;
    }

    ExceptionDetailBuilder exceptionDetails =
        createWithStackInfo(exception, parentExceptionDetails);
    exceptions.add(exceptionDetails);

    if (exception.getCause() != null) {
      convertExceptionTree(exception.getCause(), exceptionDetails, exceptions, stackSize - 1);
    }
  }

  private static ExceptionDetailBuilder createWithStackInfo(
      Throwable exception, @Nullable ExceptionDetailBuilder parentExceptionDetails) {
    if (exception == null) {
      throw new IllegalArgumentException("exception cannot be null");
    }

    ExceptionDetailBuilder exceptionDetails = new ExceptionDetailBuilder();
    exceptionDetails.setId(exception.hashCode());
    exceptionDetails.setTypeName(exception.getClass().getName());

    String exceptionMessage = exception.getMessage();
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    exceptionDetails.setMessage(exceptionMessage);

    if (parentExceptionDetails != null) {
      exceptionDetails.setOuter(parentExceptionDetails);
    }

    StackTraceElement[] trace = exception.getStackTrace();
    exceptionDetails.setHasFullStack(true);

    if (trace != null && trace.length > 0) {
      List<StackFrameBuilder> stack = new ArrayList<>();

      // We need to present the stack trace in reverse order.
      int stackLength = 0;
      for (int idx = 0; idx < trace.length; idx++) {
        StackTraceElement elem = trace[idx];

        if (elem.isNativeMethod()) {
          continue;
        }

        String className = elem.getClassName();

        StackFrameBuilder frame = new StackFrameBuilder();
        frame.setLevel(idx);
        frame.setFileName(elem.getFileName());
        frame.setLine(elem.getLineNumber());

        String method;
        if (!Strings.isNullOrEmpty(className)) {
          method = elem.getClassName() + "." + elem.getMethodName();
        } else {
          method = elem.getMethodName();
        }
        frame.setMethod(method);

        stackLength += getStackFrameLength(method, elem.getFileName(), null);
        if (stackLength > MAX_PARSED_STACK_LENGTH) {
          exceptionDetails.setHasFullStack(false);
          logger.debug(
              "parsedStack is exceeding 65536 bytes capacity. It is truncated from full {} frames to partial {} frames.",
              trace.length,
              stack.size());
          break;
        }

        stack.add(frame);
      }

      exceptionDetails.setParsedStack(stack);
    }

    return exceptionDetails;
  }

  /** Returns the stack frame length for only the strings in the stack frame. */
  // this is the same logic used to limit length on the Breeze side
  private static int getStackFrameLength(
      String method, String fileName, @Nullable String assembly) {
    return getStackFrameLength(method)
        + getStackFrameLength(fileName)
        + getStackFrameLength(assembly);
  }

  private static int getStackFrameLength(@Nullable String text) {
    return text == null ? 0 : text.length();
  }

  public static final String SAMPLING_PERCENTAGE_TRACE_STATE = "ai-internal-sp";

  private static final Cache<String, OptionalFloat> parsedSamplingPercentageCache =
      Cache.bounded(100);

  private static final AtomicBoolean alreadyLoggedSamplingPercentageMissing = new AtomicBoolean();
  private static final AtomicBoolean alreadyLoggedSamplingPercentageParseError =
      new AtomicBoolean();

  private static final Logger logger = LoggerFactory.getLogger(TelemetryUtil.class);

  public static float getSamplingPercentage(
      TraceState traceState, float defaultValue, boolean warnOnMissing) {
    String samplingPercentageStr = traceState.get(SAMPLING_PERCENTAGE_TRACE_STATE);
    if (samplingPercentageStr == null) {
      if (warnOnMissing && !alreadyLoggedSamplingPercentageMissing.getAndSet(true)) {
        // sampler should have set the trace state
        MDC.put(
            AzureMonitorMessageIdConstants.MDC_MESSAGE_ID,
            String.valueOf(AzureMonitorMessageIdConstants.SAMPLING_ERROR));
        logger.warn("did not find sampling percentage in trace state: {}", traceState);
        MDC.remove(AzureMonitorMessageIdConstants.MDC_MESSAGE_ID);
      }
      return defaultValue;
    }
    return parseSamplingPercentage(samplingPercentageStr).orElse(defaultValue);
  }

  private static OptionalFloat parseSamplingPercentage(String samplingPercentageStr) {
    return parsedSamplingPercentageCache.computeIfAbsent(
        samplingPercentageStr,
        str -> {
          try {
            return OptionalFloat.of(Float.parseFloat(str));
          } catch (NumberFormatException e) {
            if (!alreadyLoggedSamplingPercentageParseError.getAndSet(true)) {
              MDC.put(
                  AzureMonitorMessageIdConstants.MDC_MESSAGE_ID,
                  String.valueOf(AzureMonitorMessageIdConstants.SAMPLING_ERROR));
              logger.warn("error parsing sampling percentage trace state: {}", str, e);
            }
            return OptionalFloat.empty();
          } finally {
            MDC.remove(AzureMonitorMessageIdConstants.MDC_MESSAGE_ID);
          }
        });
  }

  private static class OptionalFloat {

    private static final OptionalFloat EMPTY = new OptionalFloat();

    private final boolean present;
    private final float value;

    private OptionalFloat() {
      this.present = false;
      this.value = Float.NaN;
    }

    private OptionalFloat(float value) {
      this.present = true;
      this.value = value;
    }

    public static OptionalFloat empty() {
      return EMPTY;
    }

    public static OptionalFloat of(float value) {
      return new OptionalFloat(value);
    }

    public float orElse(float other) {
      return present ? value : other;
    }
  }

  private TelemetryUtil() {}
}
