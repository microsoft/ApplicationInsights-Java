// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import javax.annotation.Nullable;

public class AiSamplerForOverride implements Sampler {

  private final SamplingPercentage samplingPercentage;

  public static AiSamplerForOverride create(SamplingPercentage samplingPercentage) {
    return new AiSamplerForOverride(samplingPercentage);
  }

  private AiSamplerForOverride(SamplingPercentage samplingPercentage) {
    this.samplingPercentage = samplingPercentage;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();
    Double parentSpanSampleRate = null;
    if (parentSpan instanceof ReadableSpan) {
      parentSpanSampleRate =
          ((ReadableSpan) parentSpan).getAttribute(AiSemanticAttributes.SAMPLE_RATE);
    }

    return internalShouldSample(parentSpanContext, parentSpanSampleRate, traceId);
  }

  public SamplingResult shouldSampleLog(SpanContext spanContext, @Nullable Double spanSampleRate) {
    return internalShouldSample(spanContext, spanSampleRate, spanContext.getTraceId());
  }

  private SamplingResult internalShouldSample(
      SpanContext parentSpanContext, @Nullable Double parentSpanSampleRate, String traceId) {

    SamplingResult samplingResult =
        useLocalParentDecisionIfPossible(parentSpanContext, parentSpanSampleRate);
    if (samplingResult != null) {
      return samplingResult;
    }

    double sp = samplingPercentage.get();

    return SamplerUtil.shouldSample(traceId, sp);
  }

  @Nullable
  private SamplingResult useLocalParentDecisionIfPossible(
      SpanContext parentSpanContext, @Nullable Double parentSpanSampleRate) {

    // remote parent-based sampling messes up item counts since item count is not propagated in
    // tracestate (yet), but local parent-based sampling doesn't have this issue since we are
    // propagating item count locally

    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      return null;
    }

    double sp = samplingPercentage.get();

    if (!parentSpanContext.isSampled()) {
      if (sp < 100) {
        // only 100% sampling override will override an unsampled parent!!
        return SamplingResult.drop();
      } else {
        // falls back in this case to sp
        return null;
      }
    }

    if (parentSpanSampleRate == null) {
      return null;
    }

    if (sp < parentSpanSampleRate || sp == 100) {
      // falls back in this case to sp
      return null;
    }
    // don't sample more dependencies than parent in this case
    return SamplerUtil.createSamplingResultWithSampleRateAndItemCount(parentSpanSampleRate);
  }

  @Override
  public String getDescription() {
    return "AiSampler";
  }
}
