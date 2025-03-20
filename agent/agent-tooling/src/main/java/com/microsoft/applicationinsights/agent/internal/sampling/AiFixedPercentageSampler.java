// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulse;
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

public class AiFixedPercentageSampler implements Sampler {

  private final double percentage;

  private static final ClientLogger logger = new ClientLogger(AiFixedPercentageSampler.class);

  private final QuickPulse quickPulse;

  public static AiFixedPercentageSampler create(double percentage, QuickPulse quickPulse) {
    return new AiFixedPercentageSampler(percentage, quickPulse);
  }

  private AiFixedPercentageSampler(double percentage, QuickPulse quickPulse) {
    this.percentage = percentage;
    this.quickPulse = quickPulse;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    String spanId = "";
    if (!parentLinks.isEmpty()) {
      spanId = parentLinks.get(0).getSpanContext().getSpanId();
    }
    logger.info("shouldSample called with traceId {}, name {}, a span id {}",
        traceId, name, spanId);
    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();
    Double parentSpanSampleRate = null;
    if (parentSpan instanceof ReadableSpan) {
      parentSpanSampleRate =
          ((ReadableSpan) parentSpan).getAttribute(AiSemanticAttributes.SAMPLE_RATE);
    }
    logger.info("caliing internalShouldSample from shouldSample with traceId {}, name {}, spanid {}",
        traceId, name, spanId);
    return internalShouldSample(parentSpanContext, parentSpanSampleRate, traceId);
  }

  public SamplingResult shouldSampleLog(SpanContext spanContext, @Nullable Double spanSampleRate) {
    logger.info("Calling internalShouldSample from shouldSampleLog with traceId {} and spanId {}", spanContext.getTraceId(), spanContext.getSpanId());
    return internalShouldSample(spanContext, spanSampleRate, spanContext.getTraceId());
  }

  private SamplingResult internalShouldSample(
      SpanContext parentSpanContext, @Nullable Double parentSpanSampleRate, String traceId) {

    SamplingResult samplingResult =
        useLocalParentDecisionIfPossible(parentSpanContext, parentSpanSampleRate);
    if (samplingResult != null) {
      logger.info("sampling result: {}", samplingResult.getDecision().toString());
      return samplingResult;
    }
    samplingResult = SamplerUtil.shouldSample(traceId, percentage, quickPulse);
    logger.info("sampling result: {}", samplingResult.getDecision().toString());
    return samplingResult;//SamplerUtil.shouldSample(traceId, percentage);
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

    if (!parentSpanContext.isSampled()) {
      if (percentage < 100) {
        if (quickPulse != null && quickPulse.isEnabled()) {
          return SamplingResult.recordOnly();
        }
        // only 100% sampling override will override an unsampled parent!!
        return SamplingResult.drop();
      } else {
        // falls back in this case to percentage
        return null;
      }
    }

    if (parentSpanSampleRate == null) {
      return null;
    }

    if (percentage < parentSpanSampleRate || percentage == 100) {
      // falls back in this case to percentage
      return null;
    }
    // don't sample more dependencies than parent in this case
    return SamplerUtil.createSamplingResultWithSampleRateAndItemCount(parentSpanSampleRate);
  }

  @Override
  public String getDescription() {
    return "FixedPercentageSampler";
  }
}
