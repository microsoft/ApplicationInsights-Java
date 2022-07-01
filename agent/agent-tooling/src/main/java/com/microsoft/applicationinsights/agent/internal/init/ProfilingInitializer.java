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

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.TempDirs;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.SdkVersionFinder;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.SystemInformation;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilerServiceInitializer;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
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

  private static ServiceProfilerServiceConfig formServiceProfilerConfig(
      Configuration.ProfilerConfiguration configuration, File tempDir) {
    URL serviceProfilerFrontEndPoint =
        TelemetryClient.getActive().getConnectionString().getProfilerEndpoint();

    // If the user has overridden their service profiler endpoint use that url
    if (configuration.serviceProfilerFrontEndPoint != null) {
      try {
        serviceProfilerFrontEndPoint = new URL(configuration.serviceProfilerFrontEndPoint);
      } catch (MalformedURLException e) {
        throw new RuntimeException(
            "Failed to parse url: " + configuration.serviceProfilerFrontEndPoint);
      }
    }

    return new ServiceProfilerServiceConfig(
        configuration.configPollPeriodSeconds,
        configuration.periodicRecordingDurationSeconds,
        configuration.periodicRecordingIntervalSeconds,
        serviceProfilerFrontEndPoint,
        configuration.memoryTriggeredSettings,
        configuration.cpuTriggeredSettings,
        TempDirs.getSubDir(tempDir, "profiles"),
        configuration.enableDiagnostics);
  }

  private ProfilingInitializer() {}
}
