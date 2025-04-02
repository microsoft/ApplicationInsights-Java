// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.core.util.logging.ClientLogger;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.RequestChecker;
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

// this sampler does two things:
// * implements same trace id hashing algorithm so that traces are sampled the same across multiple
//   nodes when some of those nodes are being monitored by other Application Insights SDKs (and 2.x
//   Java SDK)
// * adds item count to span attribute if it is sampled
public class AiSampler implements Sampler {

  private final SamplingPercentage requestSamplingPercentage;
  private final SamplingPercentage parentlessDependencySamplingPercentage;
  private final boolean ingestionSamplingEnabled;
  private static final ClientLogger logger = new ClientLogger(AiSampler.class);
  private final QuickPulse quickPulse;

  public static AiSampler create(
      SamplingPercentage requestSamplingPercentage,
      SamplingPercentage parentlessDependencySamplingPercentage,
      boolean ingestionSamplingEnabled,
      QuickPulse quickPulse) {
    return new AiSampler(
        requestSamplingPercentage,
        parentlessDependencySamplingPercentage,
        ingestionSamplingEnabled,
        quickPulse);
  }

  private AiSampler(
      SamplingPercentage requestSamplingPercentage,
      SamplingPercentage parentlessDependencySamplingPercentage,
      boolean ingestionSamplingEnabled,
      QuickPulse quickPulse) {
    this.requestSamplingPercentage = requestSamplingPercentage;
    this.parentlessDependencySamplingPercentage = parentlessDependencySamplingPercentage;
    this.ingestionSamplingEnabled = ingestionSamplingEnabled;
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
    logger.info("calling shouldsample from AISampler with traceId {}, name {}, spanId {}",
        traceId, name, spanId);
    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();
    Double parentSpanSampleRate = null;
    if (parentSpan instanceof ReadableSpan) {
      parentSpanSampleRate =
          ((ReadableSpan) parentSpan).getAttribute(AiSemanticAttributes.SAMPLE_RATE);
    }

    SamplingResult samplingResult =
        useLocalParentDecisionIfPossible(parentSpanContext, parentSpanSampleRate);
    if (samplingResult != null) {
      logger.info("sampling result: {}", samplingResult.getDecision().toString());
      return samplingResult;
    }

    double sp;
    if (requestSamplingPercentage == parentlessDependencySamplingPercentage) {
      // optimization for fixed-rate sampling
      sp = requestSamplingPercentage.get();
    } else {
      boolean isRequest = RequestChecker.isRequest(spanKind, parentSpanContext, attributes::get);
      sp =
          isRequest
              ? requestSamplingPercentage.get()
              : parentlessDependencySamplingPercentage.get();
    }

    if (sp == 100 && ingestionSamplingEnabled) {
      logger.info("sampling result: record and sample");
      return SamplingResult.recordAndSample();
    }

    samplingResult = SamplerUtil.shouldSample(traceId, sp, quickPulse);
    logger.info("sampling result: {}", samplingResult.getDecision().toString());
    return samplingResult; //SamplerUtil.shouldSample(traceId, sp);
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
      if (quickPulse != null && quickPulse.isEnabled()) {
        return SamplingResult.recordOnly();
      }
      return SamplingResult.drop();
    }
    if (parentSpanSampleRate == null) {
      return null;
    }
    return SamplerUtil.createSamplingResultWithSampleRateAndItemCount(parentSpanSampleRate);
  }

  @Override
  public String getDescription() {
    return "AiSampler";
  }
}
