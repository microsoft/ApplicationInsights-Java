// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.jfr.RecordingConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Allows loading alternative jfc configuration files. */
class AlternativeJfrConfigurations {

  private static final Logger logger = LoggerFactory.getLogger(AlternativeJfrConfigurations.class);

  public static final String REDUCED_MEMORY_PROFILE = "reduced-memory-profile.jfc";
  public static final String REDUCED_CPU_PROFILE = "reduced-cpu-profile.jfc";

  public static final String DIAGNOSTIC_MEMORY_PROFILE = "diagnostic-memory-profile.jfc";
  public static final String DIAGNOSTIC_CPU_PROFILE = "diagnostic-cpu-profile.jfc";

  private AlternativeJfrConfigurations() {}

  /** Loads a pre-set recoding file that ships with Application Insights. */
  private static RecordingConfiguration getRecordingConfiguration(
      ProfileTypes profile, String reducedProfile, String diagnosticProfile) {
    switch (profile) {
      case PROFILE_WITHOUT_ENV_DATA:
        return new RecordingConfiguration.JfcFileConfiguration(
            Objects.requireNonNull(
                AlternativeJfrConfigurations.class.getResourceAsStream(reducedProfile)));
      case DIAGNOSTIC_PROFILE:
        return new RecordingConfiguration.JfcFileConfiguration(
            Objects.requireNonNull(
                AlternativeJfrConfigurations.class.getResourceAsStream(diagnosticProfile)));
      default:
        return RecordingConfiguration.PROFILE_CONFIGURATION;
    }
  }

  /** Search for JFC file including the local file system. */
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  private static RecordingConfiguration getRecordingConfiguration(
      Configuration.ProfilerConfiguration configuration,
      @Nullable String triggeredSettings,
      AlertMetricType type) {
    if (triggeredSettings == null) {
      // Default to PROFILE_WITHOUT_ENV_DATA
      triggeredSettings = ProfileTypes.PROFILE_WITHOUT_ENV_DATA.toString();
    }

    InputStream fis = null;

    try {
      // Look for file on the local file system
      fis = new FileInputStream(triggeredSettings);
    } catch (FileNotFoundException e) {
      // NOP, to be expected if a configuration and not a file is provided
    }

    if (fis == null) {
      // Look for file in the class path
      fis = AlternativeJfrConfigurations.class.getResourceAsStream(triggeredSettings);
    }

    if (fis != null) {
      if (configuration.enableDiagnostics) {
        logger.warn(
            "Diagnostics have been enabled, however a custom JFR config was provided. This is likely to prevent diagnostics from functioning");
      }
      return new RecordingConfiguration.JfcFileConfiguration(fis);
    }

    try {
      // Try parsing the triggeredSettings as a pre-configured type
      // Convert from kebab case to enum type
      String enumType = triggeredSettings.toUpperCase(Locale.ROOT).replaceAll("-", "_");
      ProfileTypes profile = ProfileTypes.valueOf(enumType);

      profile = overlayDiagnosticSetting(configuration, profile);

      return AlternativeJfrConfigurations.get(profile, type);
    } catch (IllegalArgumentException e) {
      logger.error("Failed to find JFC configuration " + triggeredSettings);
    }

    return AlternativeJfrConfigurations.get(ProfileTypes.PROFILE_WITHOUT_ENV_DATA, type);
  }

  private static ProfileTypes overlayDiagnosticSetting(
      Configuration.ProfilerConfiguration configuration, ProfileTypes profile) {

    if (configuration.enableDiagnostics && profile != ProfileTypes.DIAGNOSTIC_PROFILE) {
      // If diagnostics are enabled then use the DIAGNOSTIC_PROFILE configuration
      if (profile != ProfileTypes.PROFILE_WITHOUT_ENV_DATA) {
        // Since DIAGNOSTIC_PROFILE and PROFILE_WITHOUT_ENV_DATA are similar, no need to warn in
        // that scenario
        logger.warn(
            "Diagnostics is enabled, this requires the use of the DIAGNOSTIC_PROFILE jfc configuration. The DIAGNOSTIC_PROFILE settings have been applied, overriding the current setting of "
                + profile.name());
      }
      return ProfileTypes.DIAGNOSTIC_PROFILE;
    }

    return profile;
  }

  static RecordingConfiguration getCpu(ProfileTypes profile) {
    return getRecordingConfiguration(profile, REDUCED_CPU_PROFILE, DIAGNOSTIC_CPU_PROFILE);
  }

  static RecordingConfiguration getMemory(ProfileTypes profile) {
    return getRecordingConfiguration(profile, REDUCED_MEMORY_PROFILE, DIAGNOSTIC_MEMORY_PROFILE);
  }

  static RecordingConfiguration getRequestConfiguration(ProfileTypes profile) {
    // Reusing the cpu profile as the most likely profile type required for a span trigger
    return getRecordingConfiguration(profile, REDUCED_CPU_PROFILE, DIAGNOSTIC_CPU_PROFILE);
  }

  static RecordingConfiguration get(ProfileTypes profile, AlertMetricType type) {
    switch (type) {
      case MEMORY:
        return getMemory(profile);
      case REQUEST:
        return getRequestConfiguration(profile);
      default:
        return getCpu(profile);
    }
  }

  static RecordingConfiguration getMemoryProfileConfig(Configuration.ProfilerConfiguration config) {
    return getRecordingConfiguration(
        config, config.memoryTriggeredSettings, AlertMetricType.MEMORY);
  }

  static RecordingConfiguration getCpuProfileConfig(Configuration.ProfilerConfiguration config) {
    return getRecordingConfiguration(config, config.cpuTriggeredSettings, AlertMetricType.CPU);
  }

  static RecordingConfiguration getSpanProfileConfig(Configuration.ProfilerConfiguration config) {
    return getRecordingConfiguration(config, config.cpuTriggeredSettings, AlertMetricType.REQUEST);
  }

  static RecordingConfiguration getManualProfileConfig(Configuration.ProfilerConfiguration config) {
    return getRecordingConfiguration(
        config, config.manualTriggeredSettings, AlertMetricType.MANUAL);
  }
}
