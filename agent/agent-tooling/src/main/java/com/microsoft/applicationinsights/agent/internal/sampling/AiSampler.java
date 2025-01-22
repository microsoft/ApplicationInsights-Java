// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.RequestChecker;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.SamplingScoreGeneratorV2;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.cache.Cache;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import javax.annotation.Nullable;

// this sampler does two things:
// * implements same trace id hashing algorithm so that traces are sampled the same across multiple
//   nodes when some of those nodes are being monitored by other Application Insights SDKs (and 2.x
//   Java SDK)
// * adds item count to span attribute if it is sampled
public class AiSampler implements Sampler {

  private static final double SAMPLE_RATE_TO_DISABLE_INGESTION_SAMPLING = 99.99;

  private final SamplingPercentage requestSamplingPercentage;
  private final SamplingPercentage parentlessDependencySamplingPercentage;
  private final boolean ingestionSamplingEnabled;
  private final boolean override;

  private final Cache<Double, SamplingResult> recordAndSampleWithSampleRateMap = Cache.bounded(100);

  public static AiSampler create(
      SamplingPercentage requestSamplingPercentage,
      SamplingPercentage parentlessDependencySamplingPercentage,
      boolean ingestionSamplingEnabled) {
    return new AiSampler(
        requestSamplingPercentage,
        parentlessDependencySamplingPercentage,
        ingestionSamplingEnabled,
        false);
  }

  public static AiSampler createSamplingOverride(
      SamplingPercentage samplingPercentage,
      boolean sampleWhenLocalParentSampled,
      boolean dropWhenLocalParentDropped) {
    return new AiSampler(samplingPercentage, samplingPercentage, false, true);
  }

  private AiSampler(
      SamplingPercentage requestSamplingPercentage,
      SamplingPercentage parentlessDependencySamplingPercentage,
      boolean ingestionSamplingEnabled,
      boolean override) {
    this.requestSamplingPercentage = requestSamplingPercentage;
    this.parentlessDependencySamplingPercentage = parentlessDependencySamplingPercentage;
    this.ingestionSamplingEnabled = ingestionSamplingEnabled;
    this.override = override;
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

    return internalShouldSample(
        parentSpanContext, parentSpanSampleRate, traceId, spanKind, attributes);
  }

  public SamplingResult shouldSampleLog(SpanContext spanContext, @Nullable Double spanSampleRate) {
    return internalShouldSample(
        spanContext,
        spanSampleRate,
        spanContext.getTraceId(),
        SpanKind.INTERNAL, // unused
        Attributes.empty());
  }

  public SamplingPercentage getParentlessDependencySamplingPercentage() {
    return parentlessDependencySamplingPercentage;
  }

  private SamplingResult internalShouldSample(
      SpanContext parentSpanContext,
      @Nullable Double parentSpanSampleRate,
      String traceId,
      SpanKind spanKind,
      Attributes attributes) {

    SamplingResult samplingResult =
        useLocalParentDecisionIfPossible(parentSpanContext, parentSpanSampleRate);
    if (samplingResult != null) {
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

    if (sp == 0) {
      return SamplingResult.drop();
    }

    if (sp != 100 && !shouldRecordAndSample(traceId, sp)) {
      return SamplingResult.drop();
    }

    if (sp == 100 && ingestionSamplingEnabled) {
      return SamplingResult.recordAndSample();
    }

    if (sp == 100) {
      // ingestion sampling is applied when sample rate is 100 (or missing)
      // so we set it to 99.99 which will bypass ingestion sampling
      // (and will still be stored as item count 1)
      sp = SAMPLE_RATE_TO_DISABLE_INGESTION_SAMPLING;
    }

    samplingResult = recordAndSampleWithSampleRateMap.get(sp);
    if (samplingResult == null) {
      samplingResult = new RecordAndSampleWithItemCount(sp);
      recordAndSampleWithSampleRateMap.put(sp, samplingResult);
    }
    return samplingResult;
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

    if (!override) {
      if (!parentSpanContext.isSampled()) {
        return SamplingResult.drop();
      }
      if (parentSpanSampleRate == null) {
        return null;
      }
      return new RecordAndSampleWithItemCount(parentSpanSampleRate);
    }

    // override case:

    // note: in the override case, requestSamplingPercentage and
    // parentlessDependencySamplingPercentage are always the same (and fixed)
    double sp = parentlessDependencySamplingPercentage.get();

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
    return new RecordAndSampleWithItemCount(parentSpanSampleRate);
  }

  public static boolean shouldRecordAndSample(String traceId, double percentage) {
    if (percentage == 100) {
      // optimization, no need to calculate score
      return true;
    }
    if (percentage == 0) {
      // optimization, no need to calculate score
      return false;
    }
    return SamplingScoreGeneratorV2.getSamplingScore(traceId) < percentage;
  }

  @Override
  public String getDescription() {
    return "AiSampler";
  }

  private static class RecordAndSampleWithItemCount implements SamplingResult {

    private final Attributes attributes;

    RecordAndSampleWithItemCount(double sampleRate) {
      attributes = Attributes.builder().put(AiSemanticAttributes.SAMPLE_RATE, sampleRate).build();
    }

    @Override
    public SamplingDecision getDecision() {
      return SamplingDecision.RECORD_AND_SAMPLE;
    }

    @Override
    public Attributes getAttributes() {
      return attributes;
    }
  }
}
