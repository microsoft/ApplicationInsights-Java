package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.List;
import javax.annotation.Nullable;

import com.microsoft.applicationinsights.agent.Exporter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this sampler does two things:
// * implements same trace id hashing algorithm so that traces are sampled the same across multiple nodes
//   when some of those nodes are being monitored by other Application Insights SDKs (and 2.x Java SDK)
// * adds sampling percentage to span attribute (TODO this is not being applied to child spans)
public final class AiSampler implements Sampler {

    private static final Logger logger = LoggerFactory.getLogger(AiSampler.class);

    // all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, ...)
    // e.g. 50 for 1/2 or 33.33 for 1/3
    //
    // failure to follow this pattern can result in unexpected / incorrect computation of values in the portal
    private final double samplingPercentage;

    private final SamplingResult alwaysOnDecision;
    private final SamplingResult alwaysOffDecision;

    public AiSampler(double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
        Attributes alwaysOnAttributes;
        if (samplingPercentage != 100) {
            alwaysOnAttributes = Attributes.of(Exporter.AI_SAMPLING_PERCENTAGE_KEY, samplingPercentage);
        } else {
            alwaysOnAttributes = Attributes.empty();
        }
        alwaysOnDecision = new FixedRateSamplerDecision(SamplingDecision.RECORD_AND_SAMPLE, alwaysOnAttributes);
        alwaysOffDecision = new FixedRateSamplerDecision(SamplingDecision.DROP, Attributes.empty());
    }

    @Override
    public SamplingResult shouldSample(@Nullable Context parentContext,
                                       String traceId,
                                       String name,
                                       SpanKind spanKind,
                                       Attributes attributes,
                                       List<LinkData> parentLinks) {
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

        private final SamplingDecision decision;
        private final Attributes attributes;

        private FixedRateSamplerDecision(SamplingDecision decision, Attributes attributes) {
            this.decision = decision;
            this.attributes = attributes;
        }

        @Override
        public SamplingDecision getDecision() {
            return decision;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }
    }
}
