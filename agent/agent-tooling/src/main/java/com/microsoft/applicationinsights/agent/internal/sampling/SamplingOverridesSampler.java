// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.RequestChecker;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulse;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

class SamplingOverridesSampler implements Sampler {

  private final SamplingOverrides requestSamplingOverrides;
  private final SamplingOverrides dependencySamplingOverrides;
  private final Sampler delegate;
  private static final ClientLogger logger = new ClientLogger(SamplingOverridesSampler.class);

  SamplingOverridesSampler(
      List<SamplingOverride> requestSamplingOverrides,
      List<SamplingOverride> dependencySamplingOverrides,
      Sampler delegate,
      QuickPulse quickPulse) {
    this.requestSamplingOverrides = new SamplingOverrides(requestSamplingOverrides, quickPulse);
    this.dependencySamplingOverrides = new SamplingOverrides(dependencySamplingOverrides, quickPulse);
    this.delegate = delegate;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    boolean isRequest = RequestChecker.isRequest(spanKind, parentSpanContext, attributes::get);

    logger.info("calling shouldsample from SamplingOverrides Sampler with traceId {}, name {}, parent spanId {}",
        traceId, name, parentSpanContext.getSpanId());

    SamplingOverrides samplingOverrides =
        isRequest ? requestSamplingOverrides : dependencySamplingOverrides;

    Sampler override = samplingOverrides.getOverride(attributes);
    if (override != null) {
      SamplingResult samplingResult =
          override.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
      logger.info("sampling result {}", samplingResult.getDecision().toString());
      return samplingResult;
    }
    SamplingResult samplingResult = delegate.shouldSample(
        parentContext, traceId, name, spanKind, attributes, parentLinks);
    logger.info("sampling result {}", samplingResult.getDecision().toString());
    return samplingResult;
  }

  @Override
  public String getDescription() {
    return "SamplingOverridesSampler";
  }
}
