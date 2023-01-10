// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.monitor.opentelemetry.exporter.implementation.RequestChecker;
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

class AiOverrideSampler implements Sampler {

  private final SamplingOverrides requestSamplingOverrides;
  private final SamplingOverrides dependencySamplingOverrides;
  private final Sampler delegate;

  AiOverrideSampler(
      List<SamplingOverride> requestSamplingOverrides,
      List<SamplingOverride> dependencySamplingOverrides,
      Sampler delegate) {
    this.requestSamplingOverrides = new SamplingOverrides(requestSamplingOverrides);
    this.dependencySamplingOverrides = new SamplingOverrides(dependencySamplingOverrides);
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

    SamplingOverrides samplingOverrides =
        isRequest ? requestSamplingOverrides : dependencySamplingOverrides;

    Sampler override = samplingOverrides.getOverride(attributes);
    if (override != null) {
      return override.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }
    return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return "AiOverrideSampler";
  }
}
