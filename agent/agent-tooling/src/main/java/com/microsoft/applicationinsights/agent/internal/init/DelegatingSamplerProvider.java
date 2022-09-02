// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@AutoService(ConfigurableSamplerProvider.class)
public class DelegatingSamplerProvider implements ConfigurableSamplerProvider {

  public static final String NAME = "lazyinit";

  @Override
  public Sampler createSampler(ConfigProperties config) {
    return DelegatingSampler.getInstance();
  }

  @Override
  public String getName() {
    return NAME;
  }
}
