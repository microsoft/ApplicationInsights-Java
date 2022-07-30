/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class Samplers {

  public static Sampler getSampler(double samplingPercentage, Configuration config) {
    SamplingOverrides samplingOverrides = new SamplingOverrides(config.preview.sampling.overrides);
    AiSampler rootSampler =
        new AiSampler(
            samplingPercentage,
            samplingOverrides,
            AiSampler.BehaviorIfNoMatchingOverrides.USE_DEFAULT_SAMPLING_PERCENTAGE);
    AiSampler parentSampledSampler =
        new AiSampler(
            samplingPercentage,
            samplingOverrides,
            AiSampler.BehaviorIfNoMatchingOverrides.RECORD_AND_SAMPLE);
    // ignoreRemoteParentNotSampled is currently needed
    // because .NET SDK always propagates trace flags "00" (not sampled)
    // NOTE: once we start propagating sampling percentage over the wire, we can use that to know
    // that we can
    // respect upstream decision for remoteParentNotSampled
    Sampler remoteParentNotSampled =
        config.preview.ignoreRemoteParentNotSampled ? rootSampler : Sampler.alwaysOff();
    return Sampler.parentBasedBuilder(rootSampler)
        .setRemoteParentNotSampled(remoteParentNotSampled)
        // this is the default, just including it for completeness
        // intentionally not allowing to capture a downstream span when upstream span has not been
        // sampled
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
