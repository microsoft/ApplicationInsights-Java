// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import org.testcontainers.containers.GenericContainer;

public class SmokeTestExtensionBuilder {

  private GenericContainer<?> dependencyContainer;
  private String dependencyContainerEnvVarName;
  private boolean usesGlobalIngestionEndpoint;
  private boolean skipHealthCheck;
  private boolean readOnly;
  private boolean useOld3xAgent;

  public SmokeTestExtensionBuilder setDependencyContainer(
      String envVarName, GenericContainer<?> container) {
    this.dependencyContainer = container;
    this.dependencyContainerEnvVarName = envVarName;
    return this;
  }

  public SmokeTestExtensionBuilder usesGlobalIngestionEndpoint() {
    this.usesGlobalIngestionEndpoint = true;
    return this;
  }

  public SmokeTestExtensionBuilder setSkipHealthCheck(boolean skipHealthCheck) {
    this.skipHealthCheck = skipHealthCheck;
    return this;
  }

  public SmokeTestExtensionBuilder setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  public SmokeTestExtensionBuilder useOld3xAgent() {
    useOld3xAgent = true;
    return this;
  }

  public SmokeTestExtension build() {
    return new SmokeTestExtension(
        dependencyContainer,
        dependencyContainerEnvVarName,
        usesGlobalIngestionEndpoint,
        skipHealthCheck,
        readOnly,
        useOld3xAgent);
  }
}
