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

package com.microsoft.applicationinsights.agent.internal.legacysdk;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.BytecodeUtilDelegate;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.models.ContextTagKeys;
import com.microsoft.applicationinsights.agent.internal.exporter.models.DataPointType;
import com.microsoft.applicationinsights.agent.internal.exporter.models.SeverityLevel;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.EventTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.ExceptionTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.MessageTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.MetricPointTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.MetricTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.PageViewTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.RequestTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.Telemetry;
import com.microsoft.applicationinsights.agent.internal.init.AiOperationNameSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingScoreGeneratorV2;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedDuration;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.tracer.ServerSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// supporting all properties of event, metric, remove dependency and page view telemetry
public class BytecodeUtilImpl implements BytecodeUtilDelegate {

  private static final Logger logger = LoggerFactory.getLogger(BytecodeUtilImpl.class);

  private static final AtomicBoolean alreadyLoggedError = new AtomicBoolean();

  public static volatile float samplingPercentage = 100;

  @Override
  public void trackEvent(
      Date timestamp,
      String name,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    EventTelemetry telemetry = EventTelemetry.create();
    TelemetryClient.getActive().populateDefaults(telemetry);

    telemetry.setName(name);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetry.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetry.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    } else {
      telemetry.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetry, tags);
    if (instrumentationKey != null) {
      telemetry.setInstrumentationKey(instrumentationKey);
    }

    track(telemetry, tags, true);
  }

  // TODO do not track if perf counter (?)
  @Override
  public void trackMetric(
      Date timestamp,
      String name,
      double value,
      Integer count,
      Double min,
      Double max,
      Double stdDev,
      Map<String, String> properties,
      Map<String, String> tags,
      String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    MetricTelemetry telemetry = MetricTelemetry.create();
    TelemetryClient.getActive().populateDefaults(telemetry);

    MetricPointTelemetry point = new MetricPointTelemetry();
    point.setName(name);
    point.setValue(value);
    point.setCount(count);
    point.setMin(min);
    point.setMax(max);
    point.setStdDev(stdDev);
    if (count != null || min != null || max != null || stdDev != null) {
      point.setDataPointType(DataPointType.AGGREGATION);
    } else {
      point.setDataPointType(DataPointType.MEASUREMENT);
    }
    telemetry.setMetricPoint(point);

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetry.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    } else {
      telemetry.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetry, tags);
    if (instrumentationKey != null) {
      telemetry.setInstrumentationKey(instrumentationKey);
    }

    track(telemetry, tags, false);
  }

  @Override
  public void trackDependency(
      Date timestamp,
      String name,
      String id,
      String resultCode,
      @Nullable Long totalMillis,
      boolean success,
      String commandName,
      String type,
      String target,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    RemoteDependencyTelemetry telemetry = RemoteDependencyTelemetry.create();
    TelemetryClient.getActive().populateDefaults(telemetry);

    telemetry.setName(name);
    if (id == null) {
      telemetry.setId(AiLegacyPropagator.generateSpanId());
    } else {
      telemetry.setId(id);
    }
    telemetry.setResultCode(resultCode);
    if (totalMillis != null) {
      telemetry.setDuration(FormattedDuration.fromMillis(totalMillis));
    }
    telemetry.setSuccess(success);
    telemetry.setData(commandName);
    telemetry.setType(type);
    telemetry.setTarget(target);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetry.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetry.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    } else {
      telemetry.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetry, tags);
    if (instrumentationKey != null) {
      telemetry.setInstrumentationKey(instrumentationKey);
    }

    track(telemetry, tags, true);
  }

  @Override
  public void trackPageView(
      Date timestamp,
      String name,
      URI uri,
      long totalMillis,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    PageViewTelemetry telemetry = PageViewTelemetry.create();
    TelemetryClient.getActive().populateDefaults(telemetry);

    telemetry.setName(name);
    if (uri != null) {
      telemetry.setUrl(uri.toString());
    }
    telemetry.setDuration(FormattedDuration.fromMillis(totalMillis));
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetry.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetry.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    } else {
      telemetry.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetry, tags);
    if (instrumentationKey != null) {
      telemetry.setInstrumentationKey(instrumentationKey);
    }

    track(telemetry, tags, true);
  }

  @Override
  public void trackTrace(
      Date timestamp,
      String message,
      int severityLevel,
      Map<String, String> properties,
      Map<String, String> tags,
      String instrumentationKey) {
    if (Strings.isNullOrEmpty(message)) {
      return;
    }
    MessageTelemetry telemetry = MessageTelemetry.create();
    TelemetryClient.getActive().populateDefaults(telemetry);

    telemetry.setMessage(message);
    if (severityLevel != -1) {
      telemetry.setSeverityLevel(getSeverityLevel(severityLevel));
    }

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetry.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    } else {
      telemetry.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetry, tags);
    if (instrumentationKey != null) {
      telemetry.setInstrumentationKey(instrumentationKey);
    }

    track(telemetry, tags, true);
  }

  @Override
  public void trackRequest(
      String id,
      String name,
      URL url,
      Date timestamp,
      @Nullable Long duration,
      String responseCode,
      boolean success,
      String source,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      String instrumentationKey) {
    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    RequestTelemetry telemetry = RequestTelemetry.create();
    TelemetryClient.getActive().populateDefaults(telemetry);

    if (id == null) {
      telemetry.setId(AiLegacyPropagator.generateSpanId());
    } else {
      telemetry.setId(id);
    }
    telemetry.setName(name);
    if (url != null) {
      telemetry.setUrl(url.toString());
    }
    if (duration != null) {
      telemetry.setDuration(FormattedDuration.fromMillis(duration));
    }
    telemetry.setResponseCode(responseCode);
    telemetry.setSuccess(success);
    telemetry.setSource(source);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetry.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetry.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    } else {
      telemetry.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetry, tags);
    if (instrumentationKey != null) {
      telemetry.setInstrumentationKey(instrumentationKey);
    }

    track(telemetry, tags, true);
  }

  @Override
  public void trackException(
      Date timestamp,
      Exception exception,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      String instrumentationKey) {
    if (exception == null) {
      return;
    }
    ExceptionTelemetry telemetry = ExceptionTelemetry.create();
    TelemetryClient.getActive().populateDefaults(telemetry);

    telemetry.setExceptions(TelemetryUtil.getExceptions(exception));
    telemetry.setSeverityLevel(SeverityLevel.ERROR);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetry.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetry.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetry.setTime(FormattedTime.offSetDateTimeFromDate(timestamp));
    } else {
      telemetry.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetry, tags);
    if (instrumentationKey != null) {
      telemetry.setInstrumentationKey(instrumentationKey);
    }

    track(telemetry, tags, true);
  }

  @Nullable
  private static SeverityLevel getSeverityLevel(int value) {
    // these mappings from the 2.x SDK
    switch (value) {
      case 0:
        return SeverityLevel.VERBOSE;
      case 1:
        return SeverityLevel.INFORMATION;
      case 2:
        return SeverityLevel.WARNING;
      case 3:
        return SeverityLevel.ERROR;
      case 4:
        return SeverityLevel.CRITICAL;
      default:
        return null;
    }
  }

  @Override
  public void flush() {
    // this is not null because sdk instrumentation is not added until TelemetryClient.setActive()
    // is called
    TelemetryClient.getActive().flushChannelBatcher().join(10, SECONDS);
  }

  @Override
  public void logErrorOnce(Throwable t) {
    if (!alreadyLoggedError.getAndSet(true)) {
      logger.error(t.getMessage(), t);
    }
  }

  private static void track(Telemetry telemetry, Map<String, String> tags, boolean applySampling) {

    String operationId = tags.get(ContextTagKeys.AI_OPERATION_ID.toString());

    SpanContext context = Span.current().getSpanContext();
    if (context.isValid()) {
      String operationParentId = tags.get(ContextTagKeys.AI_OPERATION_PARENT_ID.toString());
      String operationName = tags.get(ContextTagKeys.AI_OPERATION_NAME.toString());

      trackInsideValidSpanContext(
          telemetry, operationId, operationParentId, operationName, context, applySampling);
    } else {
      trackAsStandalone(telemetry, operationId, applySampling);
    }
  }

  private static void trackInsideValidSpanContext(
      Telemetry telemetry,
      @Nullable String operationId,
      @Nullable String operationParentId,
      @Nullable String operationName,
      SpanContext spanContext,
      boolean applySampling) {

    if (operationId != null && !operationId.equals(spanContext.getTraceId())) {
      trackAsStandalone(telemetry, operationId, applySampling);
      return;
    }

    if (!spanContext.isSampled()) {
      // sampled out
      return;
    }

    telemetry.addTag(ContextTagKeys.AI_OPERATION_ID.toString(), spanContext.getTraceId());

    if (operationParentId == null) {
      telemetry.addTag(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), spanContext.getSpanId());
    }

    if (operationName == null) {
      Span serverSpan = ServerSpan.fromContextOrNull(Context.current());
      if (serverSpan instanceof ReadableSpan) {
        telemetry.addTag(
            ContextTagKeys.AI_OPERATION_NAME.toString(),
            AiOperationNameSpanProcessor.getOperationName((ReadableSpan) serverSpan));
      }
    }

    if (applySampling) {
      float samplingPercentage =
          TelemetryUtil.getSamplingPercentage(
              spanContext.getTraceState(), BytecodeUtilImpl.samplingPercentage, false);

      if (samplingPercentage != 100) {
        telemetry.setSampleRate(samplingPercentage);
      }
    }
    // this is not null because sdk instrumentation is not added until TelemetryClient.setActive()
    // is called
    TelemetryClient.getActive().trackAsync(telemetry);
  }

  private static void trackAsStandalone(
      Telemetry telemetry, String operationId, boolean applySampling) {
    if (applySampling) {
      // sampling is done using the configured sampling percentage
      float samplingPercentage = BytecodeUtilImpl.samplingPercentage;
      if (!sample(operationId, samplingPercentage)) {
        logger.debug("Item {} sampled out", telemetry.getClass().getSimpleName());
        // sampled out
        return;
      }
      // sampled in

      if (samplingPercentage != 100) {
        telemetry.setSampleRate(samplingPercentage);
      }
    }

    // this is not null because sdk instrumentation is not added until TelemetryClient.setActive()
    // is called
    TelemetryClient.getActive().trackAsync(telemetry);
  }

  private static boolean sample(String operationId, double samplingPercentage) {
    if (samplingPercentage == 100) {
      // just an optimization
      return true;
    }
    return SamplingScoreGeneratorV2.getSamplingScore(operationId) < samplingPercentage;
  }

  private static void selectivelySetTags(Telemetry telemetry, Map<String, String> sourceTags) {
    for (Map.Entry<String, String> entry : sourceTags.entrySet()) {
      if (!entry.getKey().equals(ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString())) {
        telemetry.addTag(entry.getKey(), entry.getValue());
      }
    }
  }
}
