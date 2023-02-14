// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.smoketest.fakeingestion.ProfilerState;
import java.io.File;
import org.testcontainers.containers.GenericContainer;

public class SmokeTestExtensionBuilder {

  private GenericContainer<?> dependencyContainer;
  private String dependencyContainerEnvVarName;
  private boolean usesGlobalIngestionEndpoint;
  private boolean skipHealthCheck;
  private boolean readOnly;
  private boolean doNotSetConnectionString;
  private boolean useOld3xAgent;
  private String selfDiagnosticsLevel = "info";
  private File agentExtensionFile;
  private ProfilerState profilerEndpointPath = ProfilerState.unconfigured;

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

  public SmokeTestExtensionBuilder doNotSetConnectionString() {
    this.doNotSetConnectionString = true;
    return this;
  }

  public SmokeTestExtensionBuilder useOld3xAgent() {
    useOld3xAgent = true;
    return this;
  }

  public SmokeTestExtensionBuilder setSelfDiagnosticsLevel(String selfDiagnosticsLevel) {
    this.selfDiagnosticsLevel = selfDiagnosticsLevel;
    return this;
  }

  public SmokeTestExtension build() {
    return new SmokeTestExtension(
        dependencyContainer,
        dependencyContainerEnvVarName,
        usesGlobalIngestionEndpoint,
        skipHealthCheck,
        readOnly,
        doNotSetConnectionString,
        useOld3xAgent,
        selfDiagnosticsLevel,
        agentExtensionFile,
        profilerEndpointPath);
  }

  public SmokeTestExtensionBuilder setAgentExtensionFile(File file) {
    this.agentExtensionFile = file;
    return this;
  }

  public SmokeTestExtensionBuilder setProfilerEndpoint(ProfilerState profilerEndpointPath) {
    this.profilerEndpointPath = profilerEndpointPath;
    return this;
  }
}
