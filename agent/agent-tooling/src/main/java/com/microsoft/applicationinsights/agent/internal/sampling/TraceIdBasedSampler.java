package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.List;
import javax.annotation.Nullable;

import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
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
        if (samplingPercentage == 100) {
            alwaysOnDecision = new FixedRateSamplerDecision(Decision.RECORD_AND_SAMPLE, Attributes.empty());
        } else {
            Attributes attributes = Attributes.of(AI_SAMPLING_PERCENTAGE, samplingPercentage);
            alwaysOnDecision = new FixedRateSamplerDecision(Decision.RECORD_AND_SAMPLE, attributes);
        }
        alwaysOffDecision= new FixedRateSamplerDecision(Decision.DROP, Attributes.empty());
    }

    @Override
    public SamplingResult shouldSample(@Nullable SpanContext parentContext,
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
        return "ApplicationInsights-specific trace id based sampler, with probability: " + samplingPercentage;
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
