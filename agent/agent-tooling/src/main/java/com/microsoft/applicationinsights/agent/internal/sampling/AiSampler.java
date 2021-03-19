package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.List;
import javax.annotation.Nullable;

import com.microsoft.applicationinsights.agent.internal.sampling.SamplingOverrides.MatcherGroup;
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
class AiSampler implements Sampler {

    private static final Logger logger = LoggerFactory.getLogger(AiSampler.class);

    // all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, ...)
    // e.g. 50 for 1/2 or 33.33 for 1/3
    //
    // failure to follow this pattern can result in unexpected / incorrect computation of values in the portal
    private final double defaultSamplingPercentage;
    private final SamplingResult defaultRecordAndSampleResult;

    private final SamplingOverrides samplingOverrides;

    private final SamplingResult dropDecision;

    AiSampler(double samplingPercentage, SamplingOverrides samplingOverrides) {
        this.defaultSamplingPercentage = samplingPercentage;
        defaultRecordAndSampleResult = SamplingOverrides.getRecordAndSampleResult(defaultSamplingPercentage);

        this.samplingOverrides = samplingOverrides;

        dropDecision = SamplingResult.create(SamplingDecision.DROP, Attributes.empty());
    }

    @Override
    public SamplingResult shouldSample(@Nullable Context parentContext,
                                       String traceId,
                                       String name,
                                       SpanKind spanKind,
                                       Attributes attributes,
                                       List<LinkData> parentLinks) {

        MatcherGroup override = samplingOverrides.getOverride(attributes);

        double percentage;
        SamplingResult recordAndSampleResult;
        if (override != null) {
            percentage = override.getPercentage();
            recordAndSampleResult = override.getRecordAndSampleResult();
        } else {
            // no overrides, so fall back to the default sampling percentage
            percentage = defaultSamplingPercentage;
            recordAndSampleResult = defaultRecordAndSampleResult;
        }

        if (percentage == 100) {
            // optimization, no need to calculate score in this case
            return recordAndSampleResult;
        }
        if (percentage == 0) {
            // optimization, no need to calculate score in this case
            return dropDecision;
        }
        if (SamplingScoreGeneratorV2.getSamplingScore(traceId) >= percentage) {
            logger.debug("Item {} sampled out", name);
            return dropDecision;
        }
        return recordAndSampleResult;
    }

    @Override
    public String getDescription() {
        return "ApplicationInsights-specific trace id based sampler, with default sampling percentage: " + defaultSamplingPercentage;
    }
}
