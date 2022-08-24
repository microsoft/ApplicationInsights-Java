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
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.List;
import java.util.stream.Collectors;

public class Samplers {

  public static Sampler getSampler(Configuration config) {
    SamplingPercentage samplingPercentage;
    if (config.sampling.limitPerSecond != null) {
      samplingPercentage = SamplingPercentage.rateLimited(config.sampling.limitPerSecond);
    } else if (config.sampling.percentage != null) {
      samplingPercentage = SamplingPercentage.fixed(config.sampling.percentage);
    } else {
      throw new AssertionError("ConfigurationBuilder should have set the default sampling");
    }

    Sampler sampler = new AiSampler(samplingPercentage);

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
