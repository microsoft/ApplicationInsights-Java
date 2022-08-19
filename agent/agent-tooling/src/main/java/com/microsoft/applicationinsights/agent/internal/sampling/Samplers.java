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

import com.azure.monitor.opentelemetry.exporter.AzureMonitorSampler;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import io.opentelemetry.sdk.trace.samplers.Sampler;

public class Samplers {

  public static Sampler getSampler(float samplingPercentage, Configuration config) {
    Sampler sampler = new AzureMonitorSampler(samplingPercentage);

    if (!config.preview.sampling.overrides.isEmpty()) {
      sampler =
          new AiOverrideSampler(new SamplingOverrides(config.preview.sampling.overrides), sampler);
    }

    if (!config.preview.sampling.parentBased) {
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
