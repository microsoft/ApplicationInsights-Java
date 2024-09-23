// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.sampling;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.keytransaction.KeyTransactionConfigSupplier;
import com.microsoft.applicationinsights.agent.internal.keytransaction.KeyTransactionSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;
import java.util.stream.Collectors;

public class Samplers {

  public static Sampler getSampler(
      Configuration.Sampling sampling, Configuration.SamplingPreview samplingPreview) {
    Sampler sampler;
    if (sampling.requestsPerSecond != null) {
      SamplingPercentage requestSamplingPercentage =
          SamplingPercentage.rateLimited(sampling.requestsPerSecond);
      SamplingPercentage parentlessDependencySamplingPercentage = SamplingPercentage.fixed(100);
      sampler =
          new AiSampler(
              requestSamplingPercentage,
              parentlessDependencySamplingPercentage,
              samplingPreview.ingestionSamplingEnabled);
    } else if (sampling.percentage != null) {
      SamplingPercentage samplingPercentage = SamplingPercentage.fixed(sampling.percentage);
      sampler =
          new AiSampler(
              samplingPercentage, samplingPercentage, samplingPreview.ingestionSamplingEnabled);
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
          new AiOverrideSampler(requestSamplingOverrides, dependencySamplingOverrides, sampler);
    }

    if (samplingPreview.parentBased) {
      // when using parent-based sampling, sampling overrides still take precedence

      // IMPORTANT, the parent-based sampler is useful for interop with other sampling mechanisms,
      // as
      // it will ensure consistent traces, however it does not accurately compute item counts, since
      // item counts are not propagated in trace state (yet)
      sampler = Sampler.parentBasedBuilder(sampler).build();
    }

    if (KeyTransactionConfigSupplier.KEY_TRANSACTIONS_ENABLED) {
      sampler = KeyTransactionSampler.create(KeyTransactionConfigSupplier.getInstance(), sampler);
    }

    return sampler;
  }

  private Samplers() {}
}
