// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

public class DelegatingSampler implements Sampler {

  private static final DelegatingSampler instance = new DelegatingSampler();

  // in Azure Functions consumption pool, we don't know at startup whether to enable or not
  private volatile Sampler delegate = Sampler.alwaysOff();

  public static DelegatingSampler getInstance() {
    return instance;
  }

  public void setAlwaysOnDelegate() {
    // OpenTelemetry default sampling is "parent based", which means don't sample if remote
    // traceparent sampled flag was not set,
    // but Application Insights SDKs do not send the sampled flag (since they perform sampling
    // during export instead of head-based sampling)
    // so need to use "always on" in this case
    delegate = Sampler.alwaysOn();
  }

  public void setDelegate(Sampler delegate) {
    this.delegate = delegate;
  }

  public void reset() {
    delegate = Sampler.alwaysOff();
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return "DelegatingSampler, delegating to: " + delegate.getDescription();
  }
}
