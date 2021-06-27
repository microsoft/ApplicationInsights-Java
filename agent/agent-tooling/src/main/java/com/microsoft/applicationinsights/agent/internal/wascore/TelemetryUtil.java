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

package com.microsoft.applicationinsights.agent.internal.wascore;

import com.azure.monitor.opentelemetry.exporter.implementation.models.AvailabilityData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MessageData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.exporter.implementation.models.PageViewData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.PageViewPerfData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.RemoteDependencyData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.RequestData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.StackFrame;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionDetails;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.instrumentation.api.caching.Cache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// naming convention:
// * MonitorDomain data
// * TelemetryItem telemetry
public class TelemetryUtil {

  private static final int MAX_PARSED_STACK_LENGTH =
      32768; // Breeze will reject parsedStack exceeding 65536 bytes. Each char is 2 bytes long.

  public static TelemetryItem createMetricsTelemetry(
      TelemetryClient telemetryClient, String name, double value) {
    TelemetryItem telemetry = new TelemetryItem();
    MetricsData data = new MetricsData();
    MetricDataPoint point = new MetricDataPoint();
    telemetryClient.initMetricTelemetry(telemetry, data, point);

    point.setName(name);
    point.setValue(value);
    point.setDataPointType(DataPointType.MEASUREMENT);

    telemetry.setTime(FormattedTime.fromNow());

    return telemetry;
  }

  public static List<TelemetryExceptionDetails> getExceptions(Throwable throwable) {
    List<TelemetryExceptionDetails> exceptions = new ArrayList<>();
    convertExceptionTree(throwable, null, exceptions, Integer.MAX_VALUE);
    return exceptions;
  }

  private static void convertExceptionTree(
      Throwable exception,
      TelemetryExceptionDetails parentExceptionDetails,
      List<TelemetryExceptionDetails> exceptions,
      int stackSize) {
    if (exception == null) {
      exception = new Exception("");
    }

    if (stackSize == 0) {
      return;
    }

    TelemetryExceptionDetails exceptionDetails =
        createWithStackInfo(exception, parentExceptionDetails);
    exceptions.add(exceptionDetails);

    if (exception.getCause() != null) {
      convertExceptionTree(exception.getCause(), exceptionDetails, exceptions, stackSize - 1);
    }
  }

  private static TelemetryExceptionDetails createWithStackInfo(
      Throwable exception, TelemetryExceptionDetails parentExceptionDetails) {
    if (exception == null) {
      throw new IllegalArgumentException("exception cannot be null");
    }

    TelemetryExceptionDetails exceptionDetails = new TelemetryExceptionDetails();
    exceptionDetails.setId(exception.hashCode());
    exceptionDetails.setTypeName(exception.getClass().getName());

    String exceptionMessage = exception.getMessage();
    if (Strings.isNullOrEmpty(exceptionMessage)) {
      exceptionMessage = exception.getClass().getName();
    }
    exceptionDetails.setMessage(exceptionMessage);

    if (parentExceptionDetails != null) {
      exceptionDetails.setOuterId(parentExceptionDetails.getId());
    }

    StackTraceElement[] trace = exception.getStackTrace();
    exceptionDetails.setHasFullStack(true);

    if (trace != null && trace.length > 0) {
      List<StackFrame> stack = new ArrayList<>();

      // We need to present the stack trace in reverse order.
      int stackLength = 0;
      for (int idx = 0; idx < trace.length; idx++) {
        StackTraceElement elem = trace[idx];

        if (elem.isNativeMethod()) {
          continue;
        }

        String className = elem.getClassName();

        StackFrame frame = new StackFrame();
        frame.setLevel(idx);
        frame.setFileName(elem.getFileName());
        frame.setLine(elem.getLineNumber());

        if (!Strings.isNullOrEmpty(className)) {
          frame.setMethod(elem.getClassName() + "." + elem.getMethodName());
        } else {
          frame.setMethod(elem.getMethodName());
        }

        stackLength += getStackFrameLength(frame);
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
  private static int getStackFrameLength(StackFrame stackFrame) {
    return (stackFrame.getMethod() == null ? 0 : stackFrame.getMethod().length())
        + (stackFrame.getAssembly() == null ? 0 : stackFrame.getAssembly().length())
        + (stackFrame.getFileName() == null ? 0 : stackFrame.getFileName().length());
  }

  // TODO (trask) Azure SDK: can we move getProperties up to MonitorDomain, or if not, a common
  // interface?
  public static Map<String, String> getProperties(MonitorDomain data) {
    if (data instanceof AvailabilityData) {
      AvailabilityData availabilityData = (AvailabilityData) data;
      Map<String, String> properties = availabilityData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        availabilityData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof MessageData) {
      MessageData messageData = (MessageData) data;
      Map<String, String> properties = messageData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        messageData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof MetricsData) {
      MetricsData metricsData = (MetricsData) data;
      Map<String, String> properties = metricsData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        metricsData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof PageViewData) {
      PageViewData pageViewData = (PageViewData) data;
      Map<String, String> properties = pageViewData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        pageViewData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof PageViewPerfData) {
      PageViewPerfData pageViewPerfData = (PageViewPerfData) data;
      Map<String, String> properties = pageViewPerfData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        pageViewPerfData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof RemoteDependencyData) {
      RemoteDependencyData remoteDependencyData = (RemoteDependencyData) data;
      Map<String, String> properties = remoteDependencyData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        remoteDependencyData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof RequestData) {
      RequestData requestData = (RequestData) data;
      Map<String, String> properties = requestData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        requestData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof TelemetryEventData) {
      TelemetryEventData eventData = (TelemetryEventData) data;
      Map<String, String> properties = eventData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        eventData.setProperties(properties);
      }
      return properties;
    } else if (data instanceof TelemetryExceptionData) {
      TelemetryExceptionData exceptionData = (TelemetryExceptionData) data;
      Map<String, String> properties = exceptionData.getProperties();
      if (properties == null) {
        properties = new HashMap<>();
        exceptionData.setProperties(properties);
      }
      return properties;
    } else {
      throw new IllegalArgumentException("Unexpected type: " + data.getClass().getName());
    }
  }

  public static final String SAMPLING_PERCENTAGE_TRACE_STATE = "ai-internal-sp";

  private static final Cache<String, OptionalFloat> parsedSamplingPercentageCache =
      Cache.newBuilder().setMaximumSize(100).build();

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
        logger.warn("did not find sampling percentage in trace state: {}", traceState);
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
              logger.warn("error parsing sampling percentage trace state: {}", str, e);
            }
            return OptionalFloat.empty();
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

    public boolean isEmpty() {
      return !present;
    }
  }

  private TelemetryUtil() {}
}
