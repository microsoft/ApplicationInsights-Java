// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.bootstrap.BootstrapPackagesBuilder;
import io.opentelemetry.javaagent.tooling.bootstrap.BootstrapPackagesConfigurer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

@AutoService(BootstrapPackagesConfigurer.class)
public class AiBootstrapPackagesConfigurer implements BootstrapPackagesConfigurer {

  @Override
  public void configure(
      BootstrapPackagesBuilder bootstrapPackagesBuilder, ConfigProperties config) {
    bootstrapPackagesBuilder.add("com.microsoft.applicationinsights.agent.bootstrap");
  }
}
