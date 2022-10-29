// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilerServiceInitializer;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.io.File;

class ProfilingInitializer {

  static void initialize(
      File tempDir,
      AppIdSupplier appIdSupplier,
      Configuration config,
      TelemetryClient telemetryClient) {

    if (tempDir == null) {
      throw new FriendlyException(
          "Profile is not supported in a read-only file system.",
          "disable profiler or use a writable file system");
    }

    ProfilerServiceInitializer.initialize(
        appIdSupplier::get,
        SystemInformation.getProcessId(),
        config.role.instance,
        config.role.name,
        telemetryClient,
        formApplicationInsightsUserAgent(),
        config,
        tempDir);
  }

  private static String formApplicationInsightsUserAgent() {
    String aiVersion = SdkVersionFinder.getTheValue();
    String javaVersion = System.getProperty("java.version");
    String osName = System.getProperty("os.name");
    String arch = System.getProperty("os.arch");
    return "Microsoft-ApplicationInsights-Java-Profiler/"
        + aiVersion
        + "  (Java/"
        + javaVersion
        + "; "
        + osName
        + "; "
        + arch
        + ")";
  }

  private ProfilingInitializer() {}
}
