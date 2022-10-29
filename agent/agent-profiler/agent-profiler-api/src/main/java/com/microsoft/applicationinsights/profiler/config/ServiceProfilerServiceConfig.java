// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.profiler.config;

import com.google.auto.value.AutoValue;
import java.io.File;
import java.net.URL;

/** Configuration of the service profiler subsystem. */
@AutoValue
public abstract class ServiceProfilerServiceConfig {

  // duration between polls for configuration changes
  public abstract int getConfigPollPeriod();

  // default duration of periodic profiles
  public abstract int getPeriodicRecordingDuration();

  // default interval of periodic profiles
  public abstract int getPeriodicRecordingInterval();

  public abstract URL getServiceProfilerFrontEndPoint();

  // Either an inbuilt profile as defined in ProfileTypes, or a path to a custom JFC file to use for
  // memory profiling
  public abstract String getMemoryTriggeredSettings();

  // Either an inbuilt profile as defined in ProfileTypes, or a path to a custom JFC file to use for
  // cpu profiling
  public abstract String getCpuTriggeredSettings();

  // Either an inbuilt profile as defined in ProfileTypes, or a path to a custom JFC file to use for
  // manual profiling
  public abstract String getManualTriggeredSettings();

  // Location to which jfr files will be temporarily held
  public abstract File getTempDirectory();

  public abstract boolean isDiagnosticsEnabled();

  public static ServiceProfilerServiceConfig.Builder builder() {
    return new AutoValue_ServiceProfilerServiceConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setConfigPollPeriod(int configPollPeriod);

    public abstract Builder setPeriodicRecordingDuration(int periodicRecordingDuration);

    public abstract Builder setPeriodicRecordingInterval(int periodicRecordingInterval);

    public abstract Builder setServiceProfilerFrontEndPoint(URL serviceProfilerFrontEndPoint);

    public abstract Builder setMemoryTriggeredSettings(String memoryTriggeredSettings);

    public abstract Builder setCpuTriggeredSettings(String cpuTriggeredSettings);

    public abstract Builder setManualTriggeredSettings(String manualTriggeredSettings);

    public abstract Builder setTempDirectory(File tempDirectory);

    public abstract Builder setDiagnosticsEnabled(boolean diagnosticsEnabled);

    public abstract ServiceProfilerServiceConfig build();
  }
}
