// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.keytransaction;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class KeyTransactionSampler implements Sampler {

  private final Supplier<List<KeyTransactionConfig>> configs;
  private final Sampler fallback;

  private KeyTransactionSampler(Supplier<List<KeyTransactionConfig>> configs, Sampler fallback) {
    this.configs = configs;
    this.fallback = fallback;
  }

  public static KeyTransactionSampler create(
      Supplier<List<KeyTransactionConfig>> configs, Sampler fallback) {
    return new KeyTransactionSampler(configs, fallback);
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    SpanContext spanContext = Span.fromContext(parentContext).getSpanContext();
    if (spanContext.isValid() && !spanContext.isRemote()) {
      // for now only applying to local root spans
      return fallback.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    List<KeyTransactionConfig> configs = this.configs.get();

    Set<String> existingKeyTransactions =
        KeyTransactionTraceState.getKeyTransactionStartTimes(spanContext.getTraceState()).keySet();

    List<String> startKeyTransactions = new ArrayList<>();
    List<String> endKeyTransactions = new ArrayList<>();
    for (KeyTransactionConfig config : configs) {

      if (existingKeyTransactions.contains(config.getName())) {
        // consider ending it
        if (!config.getEndCriteria().isEmpty()
            && KeyTransactionConfig.matches(attributes, config.getEndCriteria())) {
          endKeyTransactions.add(config.getName());
        }
      } else {
        // consider starting it
        if (KeyTransactionConfig.matches(attributes, config.getStartCriteria())) {
          startKeyTransactions.add(config.getName());
          // consider ending it right away
          if (config.getEndCriteria().isEmpty()
              || KeyTransactionConfig.matches(attributes, config.getEndCriteria())) {
            endKeyTransactions.add(config.getName());
          }
        }
      }
    }

    // always delegate to fallback sampler to give it a chance to also modify trace state
    // or capture additional attributes
    SamplingResult result =
        fallback.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);

    if (existingKeyTransactions.isEmpty() && startKeyTransactions.isEmpty()) {
      return result;
    }
    return new TransactionSamplingResult(
        existingKeyTransactions, startKeyTransactions, endKeyTransactions, result);
  }

  @Override
  public String getDescription() {
    return String.format("TransactionSampler{root:%s}", fallback.getDescription());
  }

  @Override
  public String toString() {
    return getDescription();
  }

  private static class TransactionSamplingResult implements SamplingResult {

    private final Collection<String> existingKeyTransactions;
    private final Collection<String> startKeyTransactions;
    private final Collection<String> endKeyTransactions;
    private final SamplingResult delegate;

    private TransactionSamplingResult(
        Collection<String> existingKeyTransactions,
        Collection<String> startKeyTransactions,
        Collection<String> endKeyTransactions,
        SamplingResult delegate) {

      this.existingKeyTransactions = existingKeyTransactions;
      this.startKeyTransactions = startKeyTransactions;
      this.endKeyTransactions = endKeyTransactions;
      this.delegate = delegate;
    }

    @Override
    public SamplingDecision getDecision() {
      // always capture 100% of key transaction spans
      return SamplingDecision.RECORD_AND_SAMPLE;
    }

    @Override
    public Attributes getAttributes() {
      AttributesBuilder builder = delegate.getAttributes().toBuilder();

      for (String startKeyTransaction : startKeyTransactions) {
        builder.put("key_transaction." + startKeyTransaction, true);
        builder.put("key_transaction.started." + startKeyTransaction, true);
      }

      for (String existingKeyTransaction : existingKeyTransactions) {
        builder.put("key_transaction." + existingKeyTransaction, true);
      }

      for (String existingKeyTransaction : endKeyTransactions) {
        builder.put("key_transaction.ended." + existingKeyTransaction, true);
      }

      return builder.build();
    }

    @Override
    public TraceState getUpdatedTraceState(TraceState parentTraceState) {
      // TODO can we remove ended key transactions from trace state?
      //  maybe not, since the "end" span could itself still have downstream synchronous flows
      //  that need to be stamped and will complete before the "end" span itself completes

      TraceState updatedTraceState = delegate.getUpdatedTraceState(parentTraceState);

      if (startKeyTransactions.isEmpty()) {
        return updatedTraceState;
      }

      // may not match span start time exactly
      long startTime = System.currentTimeMillis();

      String newValue =
          startKeyTransactions.stream()
              .map(name -> name + ":" + startTime)
              .collect(Collectors.joining(";"));

      String existingValue = updatedTraceState.get(KeyTransactionTraceState.TRACE_STATE_KEY);

      if (existingValue != null && !existingValue.isEmpty()) {
        newValue = existingValue + ";" + newValue;
      }

      return updatedTraceState.toBuilder()
          .put(KeyTransactionTraceState.TRACE_STATE_KEY, newValue)
          .build();
    }
  }
}
