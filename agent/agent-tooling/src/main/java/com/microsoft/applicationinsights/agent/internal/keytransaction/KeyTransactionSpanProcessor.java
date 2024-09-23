// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import static io.opentelemetry.api.common.AttributeType.BOOLEAN;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeyTransactionSpanProcessor implements SpanProcessor {

  // TODO remove global state
  private static volatile DoubleHistogram keyTransactionHistogram;

  public static void initMeterProvider(MeterProvider meterProvider) {
    keyTransactionHistogram =
        meterProvider
            .get("keytransactions")
            .histogramBuilder("key_transaction.duration")
            .setDescription("Key transaction duration")
            .setUnit("s")
            .build();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {
    // copy key_transaction.<name> attributes down to child spans
    Span parentSpan = Span.fromContext(context);
    if (parentSpan instanceof ReadableSpan) {
      ((ReadableSpan) parentSpan)
          .getAttributes()
          .forEach(
              (key, value) -> {
                if (key.getKey().startsWith("key_transaction.")
                    && !key.getKey().startsWith("key_transaction.started.")
                    && !key.getKey().startsWith("key_transaction.ended.")
                    && key.getType() == BOOLEAN
                    && value instanceof Boolean) {
                  readWriteSpan.setAttribute((AttributeKey<Boolean>) key, (Boolean) value);
                }
              });
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {

    if (keyTransactionHistogram == null) {
      return;
    }

    SpanContext parentSpanContext = readableSpan.getParentSpanContext();
    if (parentSpanContext.isValid() && !parentSpanContext.isRemote()) {
      // only generating metrics for local root spans
      return;
    }

    List<String> keyTransactionNames = new ArrayList<>();
    List<String> startedKeyTransactions = new ArrayList<>();
    List<String> endedKeyTransactions = new ArrayList<>();
    readableSpan
        .getAttributes()
        .forEach(
            (attributeKey, value) -> {
              String key = attributeKey.getKey();
              if (key.startsWith("key_transaction.started.")) {
                startedKeyTransactions.add(key.substring("key_transaction.started.".length()));
              } else if (key.startsWith("key_transaction.ended.")) {
                endedKeyTransactions.add(key.substring("key_transaction.ended.".length()));
              } else if (key.startsWith("key_transaction.")) {
                keyTransactionNames.add(key.substring("key_transaction.".length()));
              }
            });

    if (keyTransactionNames.isEmpty()) {
      return;
    }

    Map<String, Long> keyTransactionStartTimes =
        KeyTransactionTraceState.getKeyTransactionStartTimes(
            readableSpan.getSpanContext().getTraceState());

    long endTime = System.currentTimeMillis();

    for (String keyTransactionName : keyTransactionNames) {
      long startTime = keyTransactionStartTimes.get(keyTransactionName);

      double duration = (endTime - startTime) / 1000.0;

      AttributesBuilder attributes =
          Attributes.builder().put("key_transaction", keyTransactionName);

      if (startedKeyTransactions.contains(keyTransactionName)) {
        attributes.put("key_transaction.started", true);
      }
      if (endedKeyTransactions.contains(keyTransactionName)) {
        attributes.put("key_transaction.ended", true);
      }

      keyTransactionHistogram.record(duration, attributes.build());
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
