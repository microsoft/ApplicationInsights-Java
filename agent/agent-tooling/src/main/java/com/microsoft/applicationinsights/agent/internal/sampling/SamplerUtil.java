// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.AiSemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.SamplingScoreGeneratorV2;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulse;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.api.internal.cache.Cache;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

public class SamplerUtil {

  private static final double SAMPLE_RATE_TO_DISABLE_INGESTION_SAMPLING = 99.99;

  private static final Cache<Double, SamplingResult> recordAndSampleWithSampleRateMap =
      Cache.bounded(100);

  static SamplingResult shouldSample(String traceId, double sp, QuickPulse quickPulse) {
    SamplingResult samplingResult;
    if (sp == 0) {
      if (quickPulse != null && quickPulse.isEnabled()) {
        return SamplingResult.recordOnly();
      }
      return SamplingResult.drop();
    }

    if (sp != 100 && !shouldRecordAndSample(traceId, sp)) {
      if (quickPulse != null && quickPulse.isEnabled()) {
        return SamplingResult.recordOnly();
      }
      return SamplingResult.drop();
    }

    if (sp == 100) {
      // ingestion sampling is applied when sample rate is 100 (or missing)
      // so we set it to 99.99 which will bypass ingestion sampling
      // (and will still be stored as item count 1)
      sp = SAMPLE_RATE_TO_DISABLE_INGESTION_SAMPLING;
    }

    samplingResult = recordAndSampleWithSampleRateMap.get(sp);
    if (samplingResult == null) {
      samplingResult = createSamplingResultWithSampleRateAndItemCount(sp);
      recordAndSampleWithSampleRateMap.put(sp, samplingResult);
    }
    return samplingResult;
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

  static SamplingResult createSamplingResultWithSampleRateAndItemCount(double sampleRate) {
    return new RecordAndSampleWithItemCount(sampleRate);
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

  private SamplerUtil() {}
}
