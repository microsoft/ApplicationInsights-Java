// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;
import java.util.stream.Collectors;

public class Samplers {

  public static Sampler getSampler(Configuration config) {
    Sampler sampler;
    if (config.sampling.requestsPerSecond != null) {
      SamplingPercentage requestSamplingPercentage =
          SamplingPercentage.rateLimited(config.sampling.requestsPerSecond);
      SamplingPercentage parentlessDependencySamplingPercentage = SamplingPercentage.fixed(100);
      sampler = new AiSampler(requestSamplingPercentage, parentlessDependencySamplingPercentage);
    } else if (config.sampling.percentage != null) {
      SamplingPercentage samplingPercentage = SamplingPercentage.fixed(config.sampling.percentage);
      sampler = new AiSampler(samplingPercentage, samplingPercentage);
    } else {
      throw new AssertionError("ConfigurationBuilder should have set the default sampling");
    }

    Configuration.SamplingPreview sampling = config.preview.sampling;

    List<SamplingOverride> requestSamplingOverrides =
        config.preview.sampling.overrides.stream()
            .filter(SamplingOverride::isForRequestTelemetry)
            .collect(Collectors.toList());
    List<SamplingOverride> dependencySamplingOverrides =
        config.preview.sampling.overrides.stream()
            .filter(SamplingOverride::isForDependencyTelemetry)
            .collect(Collectors.toList());

    if (!requestSamplingOverrides.isEmpty() || !dependencySamplingOverrides.isEmpty()) {
      sampler =
          new AiOverrideSampler(requestSamplingOverrides, dependencySamplingOverrides, sampler);
    }

    if (!sampling.parentBased) {
      return sampler;
    }

    // when using parent-based sampling, sampling overrides still take precedence

    // IMPORTANT, the parent-based sampler is useful for interop with other sampling mechanisms, as
    // it will ensure consistent traces, however it does not accurately compute item counts, since
    // item counts are not propagated in trace state (yet)
    return Sampler.parentBasedBuilder(sampler).build();
  }

  private Samplers() {}
}
