package com.microsoft.applicationinsights.agent.internal.sampling;

import io.opentelemetry.sdk.trace.samplers.Sampler;

public class Samplers {

    public static Sampler getSampler(double samplingPercentage) {
        return new AiSampler(samplingPercentage);
    }
}
