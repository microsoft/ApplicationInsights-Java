package com.microsoft.applicationinsights.agent.internal.sampling;

import io.opentelemetry.sdk.trace.samplers.Sampler;

public class Samplers {

    public static Sampler getSampler(double samplingPercentage) {
        if (samplingPercentage != 100) {
            AiSampler aiSampler = new AiSampler(samplingPercentage);
            return Sampler.parentBasedBuilder(aiSampler)
                    // for now, we have to override default behavior for "not sampled" remote parents
                    // because .NET SDK always propagates trace flags "00" (not sampled)
                    .setRemoteParentNotSampled(aiSampler)
                    .build();
        } else {
            // OpenTelemetry default sampling is "parent based", which means don't sample if remote traceparent sampled flag was not set,
            // but Application Insights SDKs do not send the sampled flag (since they perform sampling during export instead of head-based sampling)
            // so need to use "always on" in this case
            return io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn();
        }
    }
}
