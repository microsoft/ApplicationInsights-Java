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

package com.azure.monitor.opentelemetry.exporter;

import com.azure.monitor.opentelemetry.exporter.implementation.SamplingScoreGeneratorV2;
import com.azure.monitor.opentelemetry.exporter.implementation.SpanDataMapper;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
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
public class AzureMonitorSampler implements Sampler {

  private final boolean localParentBased;
  private final float samplingPercentage;
  private final SamplingResult recordAndSampleWithItemCount;

  public AzureMonitorSampler(float samplingPercentage) {
    this(samplingPercentage, true);
  }

  public AzureMonitorSampler(float samplingPercentage, boolean localParentBased) {
    int itemCount;
    if (samplingPercentage != 0) {
      itemCount = Math.round(100.0f / samplingPercentage);
      this.samplingPercentage = 100.0f / itemCount;
    } else {
      itemCount = 1;
      this.samplingPercentage = 0;
    }
    this.localParentBased = localParentBased;
    recordAndSampleWithItemCount = new RecordAndSampleWithItemCount(itemCount);
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    if (localParentBased) {
      SamplingResult samplingResult = handleLocalParent(parentContext);
      if (samplingResult != null) {
        return samplingResult;
      }
    }

    if (shouldRecordAndSample(traceId, samplingPercentage)) {
      return recordAndSampleWithItemCount;
    } else {
      return SamplingResult.drop();
    }
  }

  @Nullable
  private static SamplingResult handleLocalParent(Context parentContext) {
    // remote parent-based sampling messes up item counts since item count is not propagated in
    // tracestate (yet), but local parent-based sampling doesn't have this issue since we are
    // propagating item count locally
    Span parentSpan = Span.fromContext(parentContext);
    SpanContext parentSpanContext = parentSpan.getSpanContext();
    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      return null;
    }
    if (!parentSpanContext.isSampled()) {
      return SamplingResult.drop();
    }
    if (parentSpan instanceof ReadableSpan) {
      Long itemCount = ((ReadableSpan) parentSpan).getAttribute(SpanDataMapper.AI_ITEM_COUNT_KEY);
      if (itemCount != null) {
        return new RecordAndSampleWithItemCount(itemCount);
      }
    }
    return null;
  }

  private static boolean shouldRecordAndSample(String traceId, float percentage) {
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
    return String.format("AzureMonitorSampler{%.3f%%}", samplingPercentage);
  }

  private static class RecordAndSampleWithItemCount implements SamplingResult {

    private final Attributes attributes;

    RecordAndSampleWithItemCount(long itemCount) {
      attributes = Attributes.builder().put(SpanDataMapper.AI_ITEM_COUNT_KEY, itemCount).build();
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
