package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.List;
import javax.annotation.Nullable;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.ReadableAttributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.sdk.trace.samplers.SamplingResult.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TraceIdBasedSampler implements Sampler {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdBasedSampler.class);

    private static final AttributeKey<Double> AI_SAMPLING_PERCENTAGE = AttributeKey.doubleKey("ai.internal.sampling.percentage");

    // all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, …)
    // e.g. 50 for 1/2 or 33.33 for 1/3
    //
    // failure to follow this pattern can result in unexpected / incorrect computation of values in the portal
    private final double samplingPercentage;

    private final SamplingResult alwaysOnDecision;
    private final SamplingResult alwaysOffDecision;

    public TraceIdBasedSampler(double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
        Attributes alwaysOnAttributes;
        if (samplingPercentage != 100) {
            alwaysOnAttributes = Attributes.of(AI_SAMPLING_PERCENTAGE, samplingPercentage);
        } else {
            alwaysOnAttributes = Attributes.empty();
        }
        alwaysOnDecision = new FixedRateSamplerDecision(Decision.RECORD_AND_SAMPLE, alwaysOnAttributes);
        alwaysOffDecision= new FixedRateSamplerDecision(Decision.DROP, Attributes.empty());
    }

    @Override
    public SamplingResult shouldSample(@Nullable Context parentContext,
                                 String traceId,
                                 String name,
                                 Span.Kind spanKind,
                                 ReadableAttributes attributes,
                                 List<SpanData.Link> parentLinks) {
        if (samplingPercentage == 100) {
            return alwaysOnDecision;
        }
        if (SamplingScoreGeneratorV2.getSamplingScore(traceId) >= samplingPercentage) {
            logger.debug("Item {} sampled out", name);
            return alwaysOffDecision;
        }
        return alwaysOnDecision;
    }

    @Override
    public String getDescription() {
        return "ApplicationInsights-specific trace id based sampler, with sampling percentage: " + samplingPercentage;
    }

    private static final class FixedRateSamplerDecision implements SamplingResult {

        private final Decision decision;
        private final Attributes attributes;

        private FixedRateSamplerDecision(Decision decision, Attributes attributes) {
            this.decision = decision;
            this.attributes = attributes;
        }

        @Override
        public Decision getDecision() {
            return decision;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }
    }
}
