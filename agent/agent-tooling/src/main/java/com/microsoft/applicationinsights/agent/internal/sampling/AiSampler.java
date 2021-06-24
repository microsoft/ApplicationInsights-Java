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

import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides.MatcherGroup;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this sampler does two things:
// * implements same trace id hashing algorithm so that traces are sampled the same across multiple
// nodes
//   when some of those nodes are being monitored by other Application Insights SDKs (and 2.x Java
// SDK)
// * adds sampling percentage to span attribute (TODO this is not being applied to child spans)
class AiSampler implements Sampler {

  private static final Logger logger = LoggerFactory.getLogger(AiSampler.class);

  // all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, ...)
  // e.g. 50 for 1/2 or 33.33 for 1/3
  //
  // failure to follow this pattern can result in unexpected / incorrect computation of values in
  // the portal
  private final double defaultSamplingPercentage;
  private final SamplingResult recordAndSampleAndAddTraceStateIfMissing;

  private final SamplingOverrides samplingOverrides;

  private final SamplingResult dropDecision;

  private final BehaviorIfNoMatchingOverrides behaviorIfNoMatchingOverrides;

  // samplingPercentage is still used in BehaviorIfNoMatchingOverrides.RECORD_AND_SAMPLE
  // to set an approximate value for the span attribute
  // "applicationinsights.internal.sampling_percentage"
  //
  // in the future the sampling percentage (or its inverse "count") will be
  // carried down by trace state to set the accurate value
  AiSampler(
      double samplingPercentage,
      SamplingOverrides samplingOverrides,
      BehaviorIfNoMatchingOverrides behaviorIfNoMatchingOverrides) {
    this.defaultSamplingPercentage = samplingPercentage;
    recordAndSampleAndAddTraceStateIfMissing =
        SamplingOverrides.getRecordAndSampleAndAddTraceStateIfMissing(samplingPercentage);

    this.samplingOverrides = samplingOverrides;

    this.behaviorIfNoMatchingOverrides = behaviorIfNoMatchingOverrides;

    dropDecision = SamplingResult.create(SamplingDecision.DROP, Attributes.empty());
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    MatcherGroup override = samplingOverrides.getOverride(attributes);

    if (override != null) {
      return getSamplingResult(
          override.getPercentage(),
          override.getRecordAndSampleAndOverwriteTraceState(),
          traceId,
          name);
    }

    switch (behaviorIfNoMatchingOverrides) {
      case RECORD_AND_SAMPLE:
        // this is used for localParentSampled and remoteParentSampled
        // (note: currently sampling percentage portion of trace state is not propagated,
        //        so it will always be missing in the remoteParentSampled case)
        return recordAndSampleAndAddTraceStateIfMissing;
      case USE_DEFAULT_SAMPLING_PERCENTAGE:
        // this is used for root sampler
        return getSamplingResult(
            defaultSamplingPercentage, recordAndSampleAndAddTraceStateIfMissing, traceId, name);
    }
    throw new IllegalStateException(
        "Unexpected BehaviorIfNoMatchingOverrides: " + behaviorIfNoMatchingOverrides);
  }

  private SamplingResult getSamplingResult(
      double percentage, SamplingResult sampledSamplingResult, String traceId, String name) {
    if (percentage == 100) {
      // optimization, no need to calculate score in this case
      return sampledSamplingResult;
    }
    if (percentage == 0) {
      // optimization, no need to calculate score in this case
      return dropDecision;
    }
    if (SamplingScoreGeneratorV2.getSamplingScore(traceId) >= percentage) {
      logger.debug("Item {} sampled out", name);
      return dropDecision;
    }
    return sampledSamplingResult;
  }

  @Override
  public String getDescription() {
    return "ApplicationInsights-specific trace id based sampler, with default sampling percentage: "
        + defaultSamplingPercentage;
  }

  enum BehaviorIfNoMatchingOverrides {
    USE_DEFAULT_SAMPLING_PERCENTAGE,
    RECORD_AND_SAMPLE
  }
}
