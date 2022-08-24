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

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
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

    // TODO isRequest won't be correct for spring-scheduling, quartz and customInstrumentation
    //  since we don't have scope available in the sampler (yet)
    boolean isRequest =
        SpanDataMapper.isRequest(spanKind, parentSpanContext, null, attributes::get);

    SamplingOverrides samplingOverrides =
        isRequest ? requestSamplingOverrides : dependencySamplingOverrides;

    boolean inRequest = isRequest || parentSpanContext.isValid();
    Sampler override = samplingOverrides.getOverride(inRequest, attributes);
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
