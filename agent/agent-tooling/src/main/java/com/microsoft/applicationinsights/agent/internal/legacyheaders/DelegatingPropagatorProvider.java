// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.legacyheaders;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

@AutoService(ConfigurablePropagatorProvider.class)
public class DelegatingPropagatorProvider implements ConfigurablePropagatorProvider {

  public static final String NAME = "lazyinit";

  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    return DelegatingPropagator.getInstance();
  }

  @Override
  public String getName() {
    return NAME;
  }
}
