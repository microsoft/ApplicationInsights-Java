package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.trace.Link;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.TraceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FixedRateSampler implements Sampler {

    private static final Logger logger = LoggerFactory.getLogger(FixedRateSampler.class);

    // all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, …)
    // e.g. 50 for 1/2 or 33.33 for 1/3
    //
    // failure to follow this pattern can result in unexpected / incorrect computation of values in the portal
    private final double samplingPercentage;

    private final Decision alwaysOnDecision;
    private final Decision alwaysOffDecision;

    public FixedRateSampler(double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
        alwaysOnDecision = new FixedRateSamplerDecision(true, ImmutableMap.of("ai.sampling.percentage",
                AttributeValue.doubleAttributeValue(samplingPercentage)));
        alwaysOffDecision= new FixedRateSamplerDecision(false, Collections.<String, AttributeValue>emptyMap());
    }

    @Override
    public Decision shouldSample(@Nullable SpanContext parentContext, TraceId traceId, SpanId spanId, String name,
                                 Kind spanKind, Map<String, AttributeValue> attributes, List<Link> parentLinks) {
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

    private static final class FixedRateSamplerDecision implements Decision {

        private final boolean sampled;
        private final Map<String, AttributeValue> attributes;

        private FixedRateSamplerDecision(boolean sampled, Map<String, AttributeValue> attributes) {
            this.sampled = sampled;
            this.attributes = attributes;
        }

        @Override
        public boolean isSampled() {
            return sampled;
        }

        @Override
        public Map<String, AttributeValue> attributes() {
            return attributes;
        }
    }
}
