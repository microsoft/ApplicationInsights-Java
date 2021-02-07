package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span.Kind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

public class DelegatingSampler implements Sampler {

    private static final DelegatingSampler instance = new DelegatingSampler();

    // in Azure Functions consumption pool, we don't know at startup whether to enable or not
    private volatile Sampler delegate = Sampler.alwaysOff();

    public static DelegatingSampler getInstance() {
        return instance;
    }

    public void setDelegate(double samplingPercentage) {
        this.delegate = new AiSampler(samplingPercentage);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, Kind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "DelegatingSampler, delegating to: " + delegate.getDescription();
    }
}
