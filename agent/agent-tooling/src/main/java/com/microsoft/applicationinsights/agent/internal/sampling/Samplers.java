package com.microsoft.applicationinsights.agent.internal.sampling;

import java.util.List;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.SamplingOverride;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class Samplers {

    public static Sampler getSampler(double samplingPercentage, List<SamplingOverride> overrides) {
        SamplingOverrides samplingOverrides = new SamplingOverrides(overrides);
        AiSampler aiSampler = new AiSampler(samplingPercentage, samplingOverrides);
        return Sampler.parentBasedBuilder(aiSampler)
                // TODO change the default behavior, and provide preview flag to treat "00" as
                // for now, we have to override default behavior of "alwaysOff"
                // because .NET SDK always propagates trace flags "00" (not sampled)
                .setRemoteParentNotSampled(aiSampler)
                // this is the default, just including it for completeness
                // intentionally not allowing to capture a downstream span when upstream span has not been sampled
                // because this will lead to broken traces in A (sampled) -> B (not sampled) -> C (sampled)
                // C will point to parent B, but B will not be exported
                .setLocalParentNotSampled(Sampler.alwaysOff())
                // can filter out subtree of sampled trace, by applying sampling override
                .setRemoteParentSampled(aiSampler)
                // can filter out subtree of sampled trace, by applying sampling override
                .setLocalParentSampled(aiSampler)
                .build();
    }
}
