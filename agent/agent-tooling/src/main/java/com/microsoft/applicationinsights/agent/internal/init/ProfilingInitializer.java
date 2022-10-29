// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilerServiceInitializer;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.profiler.config.LocalConfig;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

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
        formServiceProfilerConfig(config.preview.profiler, tempDir),
        config.role.instance,
        config.role.name,
        telemetryClient,
        formApplicationInsightsUserAgent(),
        config);
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

  private static LocalConfig formServiceProfilerConfig(
      Configuration.ProfilerConfiguration configuration, File tempDir) {
    URL serviceProfilerFrontEndPoint =
        TelemetryClient.getActive().getConnectionString().getProfilerEndpoint();

    // If the user has overridden their service profiler endpoint use that url
    if (configuration.serviceProfilerFrontEndPoint != null) {
      try {
        serviceProfilerFrontEndPoint = new URL(configuration.serviceProfilerFrontEndPoint);
      } catch (MalformedURLException e) {
        throw new FriendlyException(
            "Failed to parse url: " + configuration.serviceProfilerFrontEndPoint,
            "Ensure that the service profiler endpoint is a valid url",
            e);
      }
    }

    return LocalConfig.builder()
        .setConfigPollPeriod(configuration.configPollPeriodSeconds)
        .setPeriodicRecordingDuration(configuration.periodicRecordingDurationSeconds)
        .setPeriodicRecordingInterval(configuration.periodicRecordingIntervalSeconds)
        .setServiceProfilerFrontEndPoint(serviceProfilerFrontEndPoint)
        .setMemoryTriggeredSettings(configuration.memoryTriggeredSettings)
        .setCpuTriggeredSettings(configuration.cpuTriggeredSettings)
        .setManualTriggeredSettings(configuration.manualTriggeredSettings)
        .setTempDirectory(TempDirs.getSubDir(tempDir, "profiles"))
        .setDiagnosticsEnabled(configuration.enableDiagnostics)
        .build();
  }

  private ProfilingInitializer() {}
}
