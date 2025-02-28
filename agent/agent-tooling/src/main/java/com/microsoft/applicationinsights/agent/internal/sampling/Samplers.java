// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.quickpulse.QuickPulse;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;
import java.util.stream.Collectors;

public class Samplers {

  public static Sampler getSampler(
      Configuration.Sampling sampling, Configuration.SamplingPreview samplingPreview, QuickPulse quickPulse) {
    Sampler sampler;
    if (sampling.requestsPerSecond != null) {
      SamplingPercentage requestSamplingPercentage =
          SamplingPercentage.rateLimited(sampling.requestsPerSecond);
      SamplingPercentage parentlessDependencySamplingPercentage = SamplingPercentage.fixed(100);
      sampler =
          AiSampler.create(
              requestSamplingPercentage,
              parentlessDependencySamplingPercentage,
              samplingPreview.ingestionSamplingEnabled,
              quickPulse);
    } else if (sampling.percentage != null) {
      SamplingPercentage samplingPercentage = SamplingPercentage.fixed(sampling.percentage);
      sampler =
          AiSampler.create(
              samplingPercentage, samplingPercentage, samplingPreview.ingestionSamplingEnabled, quickPulse);
    } else {
      throw new AssertionError("ConfigurationBuilder should have set the default sampling");
    }

    List<SamplingOverride> requestSamplingOverrides =
        sampling.overrides.stream()
            .filter(SamplingOverride::isForRequestTelemetry)
            .collect(Collectors.toList());
    List<SamplingOverride> dependencySamplingOverrides =
        sampling.overrides.stream()
            .filter(SamplingOverride::isForDependencyTelemetry)
            .collect(Collectors.toList());

    if (!requestSamplingOverrides.isEmpty() || !dependencySamplingOverrides.isEmpty()) {
      sampler =
          new SamplingOverridesSampler(
              requestSamplingOverrides, dependencySamplingOverrides, sampler, quickPulse);
    }

    if (!samplingPreview.parentBased) {
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
