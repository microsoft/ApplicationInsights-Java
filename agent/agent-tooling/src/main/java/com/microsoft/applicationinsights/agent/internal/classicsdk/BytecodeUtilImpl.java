// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.classicsdk;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.azure.monitor.opentelemetry.exporter.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.exporter.implementation.OperationNames;
import com.azure.monitor.opentelemetry.exporter.implementation.SamplingScoreGeneratorV2;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.AvailabilityTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.EventTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.ExceptionTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricPointBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.MetricTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.PageViewTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.RemoteDependencyTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.builders.RequestTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.SeverityLevel;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedDuration;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.BytecodeUtilDelegate;
import com.microsoft.applicationinsights.agent.internal.init.RuntimeConfiguration;
import com.microsoft.applicationinsights.agent.internal.init.RuntimeConfigurator;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyPropagator;
import com.microsoft.applicationinsights.agent.internal.statsbeat.FeatureStatsbeat;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// supporting all properties of event, metric, remove dependency and page view telemetry
public class BytecodeUtilImpl implements BytecodeUtilDelegate {

  private static final Logger logger = LoggerFactory.getLogger(BytecodeUtilImpl.class);

  private static final AtomicBoolean alreadyLoggedError = new AtomicBoolean();

  // in Azure Functions consumption pool, we don't know at startup whether to enable or not
  public static volatile float samplingPercentage = 0;

  public static volatile FeatureStatsbeat featureStatsbeat;

  public static volatile RuntimeConfigurator runtimeConfigurator;
  public static volatile boolean connectionStringConfiguredAtRuntime;

  @Override
  public void setConnectionString(String connectionString) {
    if (!connectionStringConfiguredAtRuntime) {
      logger.warn(
          "Using com.microsoft.applicationinsights.connectionstring.ConnectionString.configure()"
              + " requires setting the json configuration property"
              + " \"connectionStringConfiguredAtRuntime\" to true");
      return;
    }
    if (TelemetryClient.getActive().getConnectionString() != null) {
      logger.warn("Connection string is already set");
      return;
    }
    if (runtimeConfigurator != null) {
      RuntimeConfiguration runtimeConfig = runtimeConfigurator.getCurrentConfigCopy();
      runtimeConfig.connectionString = connectionString;
      runtimeConfigurator.apply(runtimeConfig);
    }
  }

  @Override
  public void trackEvent(
      @Nullable Date timestamp,
      String name,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    EventTelemetryBuilder telemetryBuilder = TelemetryClient.getActive().newEventTelemetryBuilder();

    telemetryBuilder.setName(name);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetryBuilder.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, true);
  }

  // TODO do not track if perf counter (?)
  @Override
  public void trackMetric(
      @Nullable Date timestamp,
      String name,
      @Nullable String namespace,
      double value,
      @Nullable Integer count,
      @Nullable Double min,
      @Nullable Double max,
      @Nullable Double stdDev,
      Map<String, String> properties,
      Map<String, String> tags,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    MetricTelemetryBuilder telemetryBuilder =
        TelemetryClient.getActive().newMetricTelemetryBuilder();

    MetricPointBuilder point = new MetricPointBuilder();
    point.setName(name);
    point.setNamespace(namespace);
    point.setValue(value);
    point.setCount(count);
    point.setMin(min);
    point.setMax(max);
    point.setStdDev(stdDev);
    telemetryBuilder.setMetricPoint(point);

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, false);
  }

  @Override
  public void trackDependency(
      @Nullable Date timestamp,
      String name,
      @Nullable String id,
      String resultCode,
      @Nullable Long duration,
      boolean success,
      String commandName,
      String type,
      String target,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    RemoteDependencyTelemetryBuilder telemetryBuilder =
        TelemetryClient.getActive().newRemoteDependencyTelemetryBuilder();

    telemetryBuilder.setName(name);
    if (id == null) {
      telemetryBuilder.setId(AiLegacyPropagator.generateSpanId());
    } else {
      telemetryBuilder.setId(id);
    }
    telemetryBuilder.setResultCode(resultCode);
    if (duration != null) {
      telemetryBuilder.setDuration(FormattedDuration.fromNanos(MILLISECONDS.toNanos(duration)));
    }
    telemetryBuilder.setSuccess(success);
    telemetryBuilder.setData(commandName);
    telemetryBuilder.setType(type);
    telemetryBuilder.setTarget(target);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetryBuilder.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, true);
  }

  @Override
  public void trackPageView(
      @Nullable Date timestamp,
      String name,
      @Nullable URI uri,
      long totalMillis,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    PageViewTelemetryBuilder telemetryBuilder =
        TelemetryClient.getActive().newPageViewTelemetryBuilder();

    telemetryBuilder.setName(name);
    if (uri != null) {
      telemetryBuilder.setUrl(uri.toString());
    }
    telemetryBuilder.setDuration(FormattedDuration.fromNanos(MILLISECONDS.toNanos(totalMillis)));
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetryBuilder.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, true);
  }

  @Override
  public void trackTrace(
      @Nullable Date timestamp,
      String message,
      int severityLevel,
      Map<String, String> properties,
      Map<String, String> tags,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (message == null) {
      return;
    }
    MessageTelemetryBuilder telemetryBuilder =
        TelemetryClient.getActive().newMessageTelemetryBuilder();

    telemetryBuilder.setMessage(message);
    if (severityLevel != -1) {
      telemetryBuilder.setSeverityLevel(getSeverityLevel(severityLevel));
    }

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, true);
  }

  @Override
  public void trackRequest(
      @Nullable String id,
      String name,
      @Nullable URL url,
      @Nullable Date timestamp,
      @Nullable Long duration,
      String responseCode,
      boolean success,
      String source,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    RequestTelemetryBuilder telemetryBuilder =
        TelemetryClient.getActive().newRequestTelemetryBuilder();

    if (id == null) {
      telemetryBuilder.setId(AiLegacyPropagator.generateSpanId());
    } else {
      telemetryBuilder.setId(id);
    }
    telemetryBuilder.setName(name);
    if (url != null) {
      telemetryBuilder.setUrl(url.toString());
    }
    if (duration != null) {
      telemetryBuilder.setDuration(FormattedDuration.fromNanos(MILLISECONDS.toNanos(duration)));
    }
    telemetryBuilder.setResponseCode(responseCode);
    telemetryBuilder.setSuccess(success);
    telemetryBuilder.setSource(source);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetryBuilder.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, true);
  }

  @Override
  public void trackException(
      @Nullable Date timestamp,
      @Nullable Throwable throwable,
      int severityLevel,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (throwable == null) {
      return;
    }
    ExceptionTelemetryBuilder telemetryBuilder =
        TelemetryClient.getActive().newExceptionTelemetryBuilder();

    telemetryBuilder.setExceptions(TelemetryUtil.getExceptions(throwable));
    if (severityLevel != -1) {
      telemetryBuilder.setSeverityLevel(getSeverityLevel(severityLevel));
    } else {
      telemetryBuilder.setSeverityLevel(SeverityLevel.ERROR);
    }
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetryBuilder.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, true);
  }

  @Override
  public void trackAvailability(
      @Nullable Date timestamp,
      String id,
      String name,
      @Nullable Long duration,
      boolean success,
      String runLocation,
      String message,
      Map<String, String> properties,
      Map<String, String> tags,
      Map<String, Double> measurements,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {

    if (Strings.isNullOrEmpty(name)) {
      return;
    }
    AvailabilityTelemetryBuilder telemetryBuilder =
        TelemetryClient.getActive().newAvailabilityTelemetryBuilder();

    telemetryBuilder.setName(name);
    if (id == null) {
      telemetryBuilder.setId(AiLegacyPropagator.generateSpanId());
    } else {
      telemetryBuilder.setId(id);
    }
    if (duration != null) {
      telemetryBuilder.setDuration(FormattedDuration.fromNanos(MILLISECONDS.toNanos(duration)));
    }
    telemetryBuilder.setSuccess(success);
    telemetryBuilder.setRunLocation(runLocation);
    telemetryBuilder.setMessage(message);
    for (Map.Entry<String, Double> entry : measurements.entrySet()) {
      telemetryBuilder.addMeasurement(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }

    if (timestamp != null) {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromEpochMillis(timestamp.getTime()));
    } else {
      telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    }
    selectivelySetTags(telemetryBuilder, tags);
    setConnectionStringOnTelemetry(telemetryBuilder, connectionString, instrumentationKey);

    track(telemetryBuilder, tags, false);
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
    TelemetryClient.getActive().forceFlush().join(10, SECONDS);
  }

  @Override
  public void logErrorOnce(Throwable t) {
    if (!alreadyLoggedError.getAndSet(true)) {
      logger.error(t.getMessage(), t);
    }
  }

  @Override
  public boolean shouldSample(String operationId) {
    return sample(operationId, samplingPercentage);
  }

  private static void track(
      AbstractTelemetryBuilder telemetryBuilder, Map<String, String> tags, boolean applySampling) {

    String existingOperationId = tags.get(ContextTagKeys.AI_OPERATION_ID.toString());

    Span span = Span.current();
    SpanContext spanContext = span.getSpanContext();

    boolean isPartOfTheCurrentTrace =
        spanContext.isValid()
            && (existingOperationId == null
                || existingOperationId.equals(spanContext.getTraceId()));

    if (isPartOfTheCurrentTrace && applySampling && !spanContext.isSampled()) {
      // no need to do anything more, sampled out
      return;
    }

    if (isPartOfTheCurrentTrace) {
      setOperationTagsFromTheCurrentSpan(
          telemetryBuilder, tags, existingOperationId, spanContext, span);
    }

    if (isPartOfTheCurrentTrace && applySampling && span instanceof ReadableSpan) {
      Long itemCount = ((ReadableSpan) span).getAttribute(AiSemanticAttributes.ITEM_COUNT);
      if (itemCount != null && itemCount != 1) {
        telemetryBuilder.setSampleRate(100.0f / itemCount);
      }
    }

    if (!isPartOfTheCurrentTrace && applySampling) {
      // standalone sampling is done using the configured sampling percentage
      float samplingPercentage = BytecodeUtilImpl.samplingPercentage;
      if (!sample(existingOperationId, samplingPercentage)) {
        logger.debug("Item {} sampled out", telemetryBuilder.getClass().getSimpleName());
        // sampled out
        return;
      }
      // sampled in

      if (samplingPercentage != 100) {
        telemetryBuilder.setSampleRate(samplingPercentage);
      }
    }

    // this is not null because sdk instrumentation is not added until TelemetryClient.setActive()
    // is called
    TelemetryClient.getActive().trackAsync(telemetryBuilder.build());

    if (featureStatsbeat != null) {
      featureStatsbeat.track2xBridgeUsage();
    }
  }

  private static void setOperationTagsFromTheCurrentSpan(
      AbstractTelemetryBuilder telemetryBuilder,
      Map<String, String> tags,
      String existingOperationId,
      SpanContext spanContext,
      Span span) {

    if (existingOperationId == null) {
      telemetryBuilder.addTag(ContextTagKeys.AI_OPERATION_ID.toString(), spanContext.getTraceId());
    }
    String existingOperationParentId = tags.get(ContextTagKeys.AI_OPERATION_PARENT_ID.toString());
    if (existingOperationParentId == null) {
      telemetryBuilder.addTag(
          ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), spanContext.getSpanId());
    }
    String existingOperationName = tags.get(ContextTagKeys.AI_OPERATION_NAME.toString());
    if (existingOperationName == null && span instanceof ReadableSpan) {
      telemetryBuilder.addTag(
          ContextTagKeys.AI_OPERATION_NAME.toString(),
          OperationNames.getOperationName((ReadableSpan) span));
    }
  }

  private static void setConnectionStringOnTelemetry(
      AbstractTelemetryBuilder telemetryBuilder,
      @Nullable String connectionString,
      @Nullable String instrumentationKey) {
    if (connectionString == null && instrumentationKey != null) {
      connectionString = "InstrumentationKey=" + instrumentationKey;
    }
    if (connectionString != null) {
      telemetryBuilder.setConnectionString(connectionString);
    }
  }

  private static boolean sample(String operationId, double samplingPercentage) {
    if (samplingPercentage == 100) {
      // just an optimization
      return true;
    }
    return SamplingScoreGeneratorV2.getSamplingScore(operationId) < samplingPercentage;
  }

  private static void selectivelySetTags(
      AbstractTelemetryBuilder telemetryBuilder, Map<String, String> sourceTags) {
    for (Map.Entry<String, String> entry : sourceTags.entrySet()) {
      if (!entry.getKey().equals(ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString())) {
        telemetryBuilder.addTag(entry.getKey(), entry.getValue());
      }
    }
  }
}
