// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestextension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.HashMap;
import java.util.Map;

public class CustomMeasurementsCustomizer implements AutoConfigurationCustomizerProvider {

  private static final AttributeKey<Value<?>> CUSTOM_MEASUREMENTS =
      AttributeKey.valueKey("microsoft.custom_measurements");

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration
        .addTracerProviderCustomizer(
            (builder, config) -> builder.addSpanProcessor(new CustomMeasurementsSpanProcessor()))
        .addLoggerProviderCustomizer(
            (builder, config) ->
                builder.addLogRecordProcessor(new CustomMeasurementsLogRecordProcessor()));
  }

  private static Value<?> customMeasurements() {
    Map<String, Value<?>> values = new HashMap<>();
    values.put("itemsProcessed", Value.of(42.0));
    values.put("queueDepth", Value.of(7.0));
    return Value.of(values);
  }

  private static class CustomMeasurementsSpanProcessor implements SpanProcessor {

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
      span.setAttribute(CUSTOM_MEASUREMENTS, customMeasurements());
    }

    @Override
    public boolean isStartRequired() {
      return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
      return false;
    }
  }

  private static class CustomMeasurementsLogRecordProcessor implements LogRecordProcessor {

    @Override
    public void onEmit(Context context, ReadWriteLogRecord logRecord) {
      logRecord.setAttribute(CUSTOM_MEASUREMENTS, customMeasurements());
    }

    @Override
    public CompletableResultCode shutdown() {
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode forceFlush() {
      return CompletableResultCode.ofSuccess();
    }
  }
}
