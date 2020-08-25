package com.microsoft.applicationinsights.agent.internal.sampling;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.trace.Link;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.TraceId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FixedRateSampler implements Sampler {

    private static final Logger logger = LoggerFactory.getLogger(FixedRateSampler.class);

    // all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, …)
    // e.g. 50 for 1/2 or 33.33 for 1/3
    //
    // failure to follow this pattern can result in unexpected / incorrect computation of values in the portal
    private final double samplingPercentage;

    private final SamplingResult alwaysOnDecision;
    private final SamplingResult alwaysOffDecision;

    public FixedRateSampler(double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
        Attributes attributes = Attributes.of("ai.sampling.percentage",
                AttributeValue.doubleAttributeValue(samplingPercentage));
        alwaysOnDecision = new FixedRateSamplerDecision(Decision.RECORD_AND_SAMPLED, attributes);
        alwaysOffDecision= new FixedRateSamplerDecision(Decision.NOT_RECORD, Attributes.empty());
    }

    @Override
    public SamplingResult shouldSample(@Nullable SpanContext parentContext,
                                 TraceId traceId,
                                 String name,
                                 Span.Kind spanKind,
                                 ReadableAttributes attributes,
                                 List<Link> parentLinks) {
        if (SamplingScoreGeneratorV2.getSamplingScore(traceId.toLowerBase16()) >= samplingPercentage) {
            logger.debug("Item {} sampled out", name);
            return alwaysOffDecision;
        }
        return alwaysOnDecision;
    }

    @Override
    public String getDescription() {
        return "fixed rate sampler: " + samplingPercentage;
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
