package com.microsoft.applicationinsights.agent.internal.sampling;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import com.microsoft.applicationinsights.agent.Exporter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
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

    private final SamplingResult downstreamCaptureResult = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
    private final SamplingResult downstreamDropResult = SamplingResult.create(SamplingDecision.DROP);
    private final SamplingResult independentCaptureResult;
    private final SamplingResult independentDropResult;

    public AiSampler(double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
        String samplingPercentageStr = toRoundedString(samplingPercentage);
        independentCaptureResult = new IndependentSamplerDecision(SamplingDecision.RECORD_AND_SAMPLE, samplingPercentageStr);
        independentDropResult = new IndependentSamplerDecision(SamplingDecision.DROP, samplingPercentageStr);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext,
                                       String traceId,
                                       String name,
                                       Span.Kind spanKind,
                                       Attributes attributes,
                                       List<LinkData> parentLinks) {
        SpanContext spanContext = Span.fromContext(parentContext).getSpanContext();
        String percentage = spanContext.getTraceState().get(Exporter.SAMPLING_PERCENTAGE_TRACE_STATE);
        if (percentage != null) {
            // ai.internal.sp is already present, which means we can honor traceflags sampling decision
            // (which normally we can't, at least when remote parent, because older AI SDKs always send traceflags=0)
            // (and also, in this case, we don't need to add current percentage to trace state, since already present)
            return spanContext.isSampled() ? downstreamCaptureResult : downstreamDropResult;
        }
        // ai.internal.sp was not sent from upstream, which means we can't honor the traceflags sampling decision
        // (since older AI SDKs always send traceflags=0)
        // (even if we could honor the traceflags in this case, we wouldn't have the sampling percentage used to
        // make that decision upstream, so we wouldn't know what "sampleRate" to apply to the telemetry envelope)

        if (samplingPercentage == 100) {
            return independentCaptureResult;
        }
        if (SamplingScoreGeneratorV2.getSamplingScore(traceId) >= samplingPercentage) {
            logger.debug("Item {} sampled out", name);
            return independentDropResult;
        }
        return independentCaptureResult;
    }

    @Override
    public String getDescription() {
        return "ApplicationInsights-specific trace id based sampler, with sampling percentage: " + samplingPercentage;
    }

    // TODO write test for
    //  * 33.33333333333
    //  * 66.66666666666
    //  * 1.123456
    //  * 50.0
    //  * 1.0
    //  * 0
    //  * 0.001
    //  * 0.000001
    // 5 digit of precision, and remove any trailing zeros beyond the decimal point
    private static String toRoundedString(double percentage) {
        BigDecimal bigDecimal = new BigDecimal(percentage);
        bigDecimal = bigDecimal.round(new MathContext(5));
        String formatted = bigDecimal.toString();
        double dv = bigDecimal.doubleValue();
        if (dv > 0 && dv < 1) {
            while (formatted.endsWith("0")) {
                formatted = formatted.substring(0, formatted.length() - 1);
            }
        }
        return formatted;
    }

    private static final class IndependentSamplerDecision implements SamplingResult {

        private final SamplingDecision decision;
        private final String samplingPercentage;
        private final TraceState traceState;

        private IndependentSamplerDecision(SamplingDecision decision, String samplingPercentage) {
            this.decision = decision;
            this.samplingPercentage = samplingPercentage;
            traceState = TraceState.builder().set(Exporter.SAMPLING_PERCENTAGE_TRACE_STATE, samplingPercentage).build();
        }

        @Override
        public SamplingDecision getDecision() {
            return decision;
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.empty();
        }

        @Override
        public TraceState getUpdatedTraceState(TraceState parentTraceState) {
            if (parentTraceState.isEmpty()) {
                return traceState;
            } else if (parentTraceState.get(Exporter.SAMPLING_PERCENTAGE_TRACE_STATE) != null) {
                return parentTraceState;
            }
            return parentTraceState.toBuilder().set(Exporter.SAMPLING_PERCENTAGE_TRACE_STATE, samplingPercentage).build();
        }
    }
}
