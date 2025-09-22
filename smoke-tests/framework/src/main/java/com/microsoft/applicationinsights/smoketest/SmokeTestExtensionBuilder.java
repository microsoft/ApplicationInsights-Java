// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.smoketest.fakeingestion.ProfilerState;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;

public class SmokeTestExtensionBuilder {

  private GenericContainer<?> dependencyContainer;
  private String dependencyContainerEnvVarName;
  private boolean usesGlobalIngestionEndpoint;
  private boolean skipHealthCheck;
  private boolean readOnly;
  private boolean doNotSetConnectionString;
  private String otelResourceAttributesEnvVar;
  private boolean useOld3xAgent;
  private String selfDiagnosticsLevel = "info";
  private File agentExtensionFile;
  private ProfilerState profilerEndpointPath = ProfilerState.unconfigured;
  private final Map<String, String> httpHeaders = new HashMap<>();
  private final Map<String, String> envVars = new HashMap<>();
  private final List<String> jvmArgs = new ArrayList<>();
  private boolean useDefaultHttpPort;
  private boolean useOtlpEndpoint;

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

  public SmokeTestExtensionBuilder otelResourceAttributesEnvVar(
      String otelResourceAttributesEnvVar) {
    this.otelResourceAttributesEnvVar = otelResourceAttributesEnvVar;
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

  public SmokeTestExtensionBuilder setAgentExtensionFile(File file) {
    this.agentExtensionFile = file;
    return this;
  }

  public SmokeTestExtensionBuilder setProfilerEndpoint(ProfilerState profilerEndpointPath) {
    this.profilerEndpointPath = profilerEndpointPath;
    return this;
  }

  public SmokeTestExtensionBuilder setHttpHeader(String key, String value) {
    httpHeaders.put(key, value);
    return this;
  }

  public SmokeTestExtensionBuilder setEnvVar(String name, String value) {
    envVars.put(name, value);
    return this;
  }

  public SmokeTestExtensionBuilder addJvmArg(String jvmArg) {
    jvmArgs.add(jvmArg);
    return this;
  }

  public SmokeTestExtensionBuilder setUseDefaultHttpPort() {
    this.useDefaultHttpPort = true;
    return this;
  }

  public SmokeTestExtensionBuilder useOtlpEndpoint() {
    this.useOtlpEndpoint = true;
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
        otelResourceAttributesEnvVar,
        useOld3xAgent,
        selfDiagnosticsLevel,
        agentExtensionFile,
        profilerEndpointPath,
        httpHeaders,
        envVars,
        jvmArgs,
        useDefaultHttpPort,
        useOtlpEndpoint);
  }
}
