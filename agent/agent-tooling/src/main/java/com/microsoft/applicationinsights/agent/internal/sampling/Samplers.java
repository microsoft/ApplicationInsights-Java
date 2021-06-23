package com.microsoft.applicationinsights.agent.internal.sampling;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class Samplers {

    public static Sampler getSampler(double samplingPercentage, Configuration config) {
        SamplingOverrides samplingOverrides = new SamplingOverrides(config.preview.sampling.overrides);
        AiSampler rootSampler = new AiSampler(samplingPercentage, samplingOverrides,
                AiSampler.BehaviorIfNoMatchingOverrides.USE_DEFAULT_SAMPLING_PERCENTAGE);
        AiSampler parentSampledSampler = new AiSampler(samplingPercentage, samplingOverrides,
                AiSampler.BehaviorIfNoMatchingOverrides.RECORD_AND_SAMPLE);
        // ignoreRemoteParentNotSampled is currently needed
        // because .NET SDK always propagates trace flags "00" (not sampled)
        // NOTE: once we start propagating sampling percentage over the wire, we can use that to know that we can
        // respect upstream decision for remoteParentNotSampled
        Sampler remoteParentNotSampled = config.preview.ignoreRemoteParentNotSampled ? rootSampler : Sampler.alwaysOff();
        return Sampler.parentBasedBuilder(rootSampler)
                .setRemoteParentNotSampled(remoteParentNotSampled)
                // this is the default, just including it for completeness
                // intentionally not allowing to capture a downstream span when upstream span has not been sampled
                // because this will lead to broken traces in A (sampled) -> B (not sampled) -> C (sampled)
                // C will point to parent B, but B will not be exported
                .setLocalParentNotSampled(Sampler.alwaysOff())
                // can filter out subtree of sampled trace, by applying sampling override
                .setRemoteParentSampled(parentSampledSampler)
                // can filter out subtree of sampled trace, by applying sampling override
                .setLocalParentSampled(parentSampledSampler)
                .build();
    }

    private Samplers() {}
}
